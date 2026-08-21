package com.v7878.fee0.bluetooth;

import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.Build.VERSION_CODES;

import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressLint("MissingPermission")
public class BLEDeviceManager {
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int MTU_REQUEST = 255;

    public interface WriteCallback {
        void onResult(boolean success);
    }

    private static class PacketTask {
        final BluetoothGattCharacteristic target;
        final byte[] data;
        private WriteCallback callback;

        PacketTask(BluetoothGattCharacteristic target, byte[] data, WriteCallback callback) {
            this.target = Objects.requireNonNull(target);
            this.data = Objects.requireNonNull(data);
            this.callback = callback;
        }

        public void onResult(boolean status) {
            if (callback != null) {
                callback.onResult(status);
                callback = null;
            }
        }
    }

    public interface DeviceCallback {
        void onConnecting();

        void onConnected();

        void onServicesDiscovered(List<BluetoothGattService> services);

        void onNotify(BluetoothGattCharacteristic ch, byte[] raw);

        void onWrite(BluetoothGattCharacteristic ch, byte[] raw, boolean success);

        void onDisconnected();
    }

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;

    private DeviceCallback callback;
    private BluetoothGatt gatt;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isWriting = new AtomicBoolean(false);
    private final AtomicReference<PacketTask> currentPacket = new AtomicReference<>(null);
    private final Queue<PacketTask> commandQueue = new ConcurrentLinkedQueue<>();

    public BLEDeviceManager(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        this.bluetoothAdapter = manager != null ? manager.getAdapter() : null;
    }

    private void resetState() {
        isConnected.set(false);
        isWriting.set(false);
        currentPacket.set(null);
    }

    public void connect(String macAddress, DeviceCallback callback) {
        if (isConnected.get()) throw new IllegalStateException("Already connected");
        this.callback = callback;
        callback.onConnecting();
        resetState();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        //noinspection deprecation TODO
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void disconnect() {
        commandQueue.clear();
        if (gatt != null) {
            try {
                gatt.disconnect();
            } catch (Exception ignored) {
            }
            gatt = null;
        }
        resetState();
    }

    @SuppressWarnings("deprecation")
    private boolean writeDesc(BluetoothGatt g, BluetoothGattDescriptor desc, byte[] value) {
        Objects.requireNonNull(g);
        Objects.requireNonNull(desc);
        Objects.requireNonNull(value);

        if (SDK_INT >= VERSION_CODES.TIRAMISU) {
            try {
                return g.writeDescriptor(desc, value) == BluetoothStatusCodes.SUCCESS;
            } catch (Exception e) {
                return false;
            }
        }
        desc.setValue(value);
        return g.writeDescriptor(desc);
    }

    public boolean enableNotifications(BluetoothGattCharacteristic ch, boolean enable) {
        Objects.requireNonNull(ch);

        var g = gatt;
        if (g == null) return false;
        g.setCharacteristicNotification(ch, enable);
        BluetoothGattDescriptor desc = ch.getDescriptor(CCCD_UUID);
        if (desc == null) return false;
        return writeDesc(g, desc, enable
                ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }

    public void queueCommand(BluetoothGattCharacteristic target, byte[] data, WriteCallback callback) {
        if (!commandQueue.offer(new PacketTask(target, data, callback))) {
            throw new AssertionError();
        }
        if (!isWriting.get()) processNextCommand();
    }

    public void queueCommand(BluetoothGattCharacteristic target, byte... data) {
        queueCommand(target, data, null);
    }

    @SuppressWarnings({"deprecation", "SameParameterValue"})
    private boolean writeChar(BluetoothGatt g, BluetoothGattCharacteristic ch, byte[] value, int type) {
        Objects.requireNonNull(g);
        Objects.requireNonNull(ch);
        Objects.requireNonNull(value);

        if (SDK_INT >= VERSION_CODES.TIRAMISU) {
            try {
                return g.writeCharacteristic(ch, value, type) == BluetoothStatusCodes.SUCCESS;
            } catch (Exception e) {
                return false;
            }
        }
        ch.setWriteType(WRITE_TYPE_NO_RESPONSE);
        ch.setValue(value);
        return g.writeCharacteristic(ch);
    }

    private void processNextCommand() {
        PacketTask task = commandQueue.poll();
        if (task == null) {
            isWriting.set(false);
            return;
        }
        var g = gatt;
        if (g == null || !isConnected.get()) {
            task.onResult(false);
            return;
        }

        isWriting.set(true);
        currentPacket.set(task);

        if (!writeChar(g, task.target, task.data, WRITE_TYPE_NO_RESPONSE)) {
            task.onResult(false);
            processNextCommand();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected.set(true);
                g.requestMtu(MTU_REQUEST);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                callback.onDisconnected();
                try {
                    g.close();
                } catch (Exception ignored) {
                }
                gatt = null;
                resetState();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                g.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            callback.onServicesDiscovered(gatt.getServices());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
            if (CCCD_UUID.equals(descriptor.getUuid()) && status == BluetoothGatt.GATT_SUCCESS) {
                callback.onConnected();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic ch, byte[] value) {
            if (value == null || value.length == 0) return;

            callback.onNotify(ch, value.clone());
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic ch) {
            onCharacteristicChanged(g, ch, ch.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic ch, int status) {
            PacketTask task = currentPacket.getAndSet(null);
            assert task != null;

            var success = status == BluetoothGatt.GATT_SUCCESS;
            callback.onWrite(ch, task.data, success);
            task.onResult(success);

            processNextCommand();
        }
    };
}
