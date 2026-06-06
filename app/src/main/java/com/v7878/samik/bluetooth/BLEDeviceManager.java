package com.v7878.samik.bluetooth;

import static com.v7878.samik.bluetooth.ScooterManager.TAG;

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
import android.content.Context;
import android.util.Log;

import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressLint("MissingPermission")
public class BLEDeviceManager {
    private static final UUID SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID IO_CHAR_UUID = UUID.fromString("0000fee2-0000-1000-8000-00805f9b34fb");
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int MTU_REQUEST = 255;

    public interface WriteCallback {
        void onResult(boolean success);
    }

    private static class PacketTask {
        private WriteCallback callback;
        final byte cmd;
        final byte[] data;

        PacketTask(byte cmd, byte[] data, WriteCallback callback) {
            this.cmd = cmd;
            this.data = data;
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
        void onAttach(BLEDeviceManager manager);

        void onConnecting();

        void onConnected();

        void onDisconnected();

        void onNotify(int cmd, byte[] data, int status, boolean isValid);

        void onRawNotify(byte[] raw);

        void onRawWrite(byte[] raw, boolean success);
    }

    private final Context context;
    private final DeviceCallback callback;
    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic io;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isWriting = new AtomicBoolean(false);
    private final AtomicReference<PacketTask> currentPacket = new AtomicReference<>(null);
    private final Queue<PacketTask> commandQueue = new ConcurrentLinkedQueue<>();

    public BLEDeviceManager(Context context, DeviceCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = Objects.requireNonNull(callback);
        callback.onAttach(this);
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        this.bluetoothAdapter = manager != null ? manager.getAdapter() : null;
    }

    private void resetState() {
        isConnected.set(false);
        isWriting.set(false);
        currentPacket.set(null);
    }

    public void connect(String macAddress) {
        if (isConnected.get()) return;
        callback.onConnecting();
        resetState();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void disconnect() {
        commandQueue.clear();
        if (gatt != null) {
            try {
                gatt.disconnect();
            } catch (Exception ignored) {
            }
            try {
                gatt.close();
            } catch (Exception ignored) {
            }
            gatt = null;
        }
        io = null;
        resetState();
    }

    @SuppressWarnings("SameParameterValue")
    private void enableNotifications(boolean enable) {
        gatt.setCharacteristicNotification(io, enable);
        BluetoothGattDescriptor desc = io.getDescriptor(CCCD_UUID);
        if (desc != null) {
            desc.setValue(enable
                    ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }
    }

    public void queueCommand(byte cmd, byte... data) {
        queueCommand(cmd, data, success -> {
            Log.i(TAG, String.format("Writed: %02X ", cmd & 0xFF) + success);
        });
    }

    public void queueCommand(byte cmd, byte[] data, WriteCallback callback) {
        if (!commandQueue.offer(new PacketTask(cmd, data, callback))) {
            throw new AssertionError();
        }
        if (!isWriting.get()) processNextCommand();
    }

    private void processNextCommand() {
        PacketTask task = commandQueue.poll();
        if (task == null) {
            isWriting.set(false);
            return;
        }
        if (io == null || gatt == null || !isConnected.get()) {
            task.onResult(false);
            return;
        }

        isWriting.set(true);
        currentPacket.set(task);

        byte[] packet = buildOutgoingPacket(task.cmd, task.data);

        io.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        io.setValue(packet);

        if (!gatt.writeCharacteristic(io)) {
            task.onResult(false);
            processNextCommand();
        }
    }

    // [FA AF A5 5A] [CMD] [LEN] [DATA...] [CHECKSUM]
    private byte[] buildOutgoingPacket(byte cmd, byte[] data) {
        int dataLen = (data != null) ? data.length : 0;
        if (dataLen > (255 - 7)) {
            throw new IllegalArgumentException("Too much data: " + dataLen);
        }
        byte[] packet = new byte[4 + 1 + 1 + dataLen + 1];
        packet[0] = (byte) 0xFA;
        packet[1] = (byte) 0xAF;
        packet[2] = (byte) 0xA5;
        packet[3] = (byte) 0x5A;
        packet[4] = cmd;
        packet[5] = (byte) dataLen;
        if (data != null) System.arraycopy(data, 0, packet, 6, dataLen);

        int checksum = 0x5A + (cmd & 0xFF) + (dataLen & 0xFF);
        for (int i = 0; i < dataLen; i++) checksum += (data[i] & 0xFF);
        packet[packet.length - 1] = (byte) checksum;

        return packet;
    }

    // [5A] [CMD] [LEN] [DATA...] [CHECKSUM] [STATUS]
    private void parseIncomingPacket(byte[] raw) {
        if (raw.length < 5 || (raw[0] & 0xFF) != 0x5A) return;

        int cmd = raw[1] & 0xFF;
        int len = raw[2] & 0xFF;
        int expectedTotal = 3 + len + 2;
        if (raw.length < expectedTotal) return;

        int calc = 0x5A + cmd + len;
        for (int i = 0; i < len; i++) calc += (raw[3 + i] & 0xFF);
        calc &= 0xFF;
        int recvChecksum = raw[3 + len] & 0xFF;
        int status = raw[3 + len + 1] & 0xFF;

        byte[] data = new byte[len];
        System.arraycopy(raw, 3, data, 0, len);

        callback.onNotify(cmd, data, status, calc == recvChecksum);
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
                io = null;
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
            BluetoothGattService service = g.getService(SERVICE_UUID);
            if (service == null) {
                return;
            }
            io = service.getCharacteristic(IO_CHAR_UUID);
            if (io == null) {
                return;
            }
            enableNotifications(true);
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

            callback.onRawNotify(value.clone());

            parseIncomingPacket(value);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic ch, int status) {
            PacketTask task = currentPacket.getAndSet(null);
            assert task != null;

            var success = status == BluetoothGatt.GATT_SUCCESS;

            var written = ch.getValue();
            written = written != null ? written.clone() : new byte[0];
            callback.onRawWrite(written, success);

            task.onResult(success);

            processNextCommand();
        }
    };
}
