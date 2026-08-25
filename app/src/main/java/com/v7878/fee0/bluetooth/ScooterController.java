package com.v7878.fee0.bluetooth;

import static com.v7878.fee0.ConnectionState.CONNECTED;
import static com.v7878.fee0.ConnectionState.CONNECTING;
import static com.v7878.fee0.ConnectionState.DISCONNECTED;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.BINDING;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.BRAKE_STRENGTH;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.GEAR;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.MAX_CURRENT;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.MAX_SPEED;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.STARTING_TORQUE;
import static com.v7878.fee0.bluetooth.ScooterController.Commands.START_TELEMETRY;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.v7878.fee0.ConnectionState;
import com.v7878.fee0.ScooterState;
import com.v7878.fee0.bluetooth.BLEDeviceManager.DeviceCallback;

import java.util.Arrays;
import java.util.UUID;

public class ScooterController {
    public interface ScooterCallback {
        void onTelemetryUpdate(ScooterState telemetry);
    }

    public static class Commands {
        public static final byte START_TELEMETRY = (byte) 0x01;
        public static final byte PARKING_MODE = (byte) 0x33;
        public static final byte SEARCH = (byte) 0x34;
        public static final byte KICK_START = (byte) 0x35;
        public static final byte CRUISE_CONTROL = (byte) 0x36;
        public static final byte MAX_SPEED = (byte) 0x3C;
        public static final byte STARTING_TORQUE = (byte) 0x3D;
        public static final byte MAX_CURRENT = (byte) 0x3E;
        public static final byte BRAKE_STRENGTH = (byte) 0x3F;
        public static final byte GEAR = (byte) 0x42;
        public static final byte UNITS = (byte) 0x43;
        public static final byte LIGHT = (byte) 0x45;
        public static final byte BINDING = (byte) 0x4C;
    }

    public static class ScooterTelemetry {
        public ConnectionState connection = DISCONNECTED;

        // 0x10
        public Float voltage;
        public Float current;
        public Float speed;
        public Integer batteryPercent;
        public Integer controllerTemp;
        public Integer motorTemp;
        public Integer batteryTemp;
        public Integer motorRpm;

        // 0x11
        // flags1
        public Integer gear;
        public Boolean lights;
        public Boolean taillight;
        public Boolean startingMode;
        public Boolean cruiseControl;
        public Boolean unitMph;

        // flags2
        public Boolean switchControl;
        public Boolean locked;
        public Boolean horn;
        public Boolean leftTurn;
        public Boolean rightTurn;
        public Boolean ambientLight;
        public Boolean bluetoothBound;
        public Boolean reserved_byte_1_bit_7;

        public Float singleMileage;
        public Integer totalMileage;
        public Integer lightValue;

        // flags3
        public Boolean cruisingCondition;
        public Boolean brakingState;
        public Boolean lockCondition;
        public Boolean abnormalCommunication;
        public Boolean batteryOvervoltage;
        public Boolean batteryUndervoltage;
        public Boolean motorPhaseFault;
        public Boolean chargingState;

        // flags4
        public Boolean lockedRotorFault;
        public Boolean hardwareOvercurrent;
        public Boolean controllerFailure;
        public Boolean throttleFault;
        public Boolean brakeSensorFault;
        public Boolean motorHalfFault;
        public Boolean reserved_byte_9_bit_6;
        public Boolean reserved_byte_9_bit_7;

        // 0x12
        public Integer instrumentMCUID, instrumentHwVersion, instrumentSwVersion;
        public Integer controllerMCUID, controllerHwVersion, controllerSwVersion;
        public Integer gearsBitmask;

        // 0x13
        public Integer throttleValue, brakeVal1, brakeVal2;
        public Integer throttleVal_CY, brakeVal1_CY, brakeVal2_CY;

        // 0x16
        public Integer batteryCutoffVoltage;
        public Integer batteryLevel1Voltage;
        public Integer batteryLevel2Voltage;
        public Integer batteryLevel3Voltage;
        public Integer batteryLevel4Voltage;
        public Integer batteryLevel5Voltage;

        // 0x3c - 0x3f
        public Integer maxSpeed, maxSpeedLimit;
        public Integer startTorque, startTorqueLimit;
        public Integer maxCurrent, maxCurrentLimit;
        public Integer brakeStrength, brakeStrengthLimit;

        public boolean hasErrors() {
            return lockedRotorFault || hardwareOvercurrent || controllerFailure
                    || throttleFault || brakeSensorFault || motorHalfFault
                    || motorPhaseFault || batteryOvervoltage || batteryUndervoltage
                    || abnormalCommunication;
        }

        public ScooterState toState() {
            return new ScooterState(
                    connection,
                    voltage,
                    current,
                    speed,
                    motorRpm,
                    gear,
                    unitMph != null && unitMph,
                    singleMileage,
                    totalMileage,
                    controllerTemp,
                    motorTemp,
                    batteryTemp,
                    throttleValue,
                    brakeVal1,
                    brakeVal2
            );
        }
    }

    public static final UUID SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID IO_CHAR_UUID = UUID.fromString("0000fee2-0000-1000-8000-00805f9b34fb");

    public static final int GEAR_ECO = 1;
    public static final int GEAR_D = 2;
    public static final int GEAR_S = 3;
    public static final int GEAR_WALK = 4;

    private final BLEDeviceManager manager;
    private volatile ScooterTelemetry telemetry;
    private volatile ScooterCallback callback;
    private volatile BluetoothGattCharacteristic io;

    public ScooterController(Context context) {
        manager = new BLEDeviceManager(context);
    }

    public void connect(BluetoothDevice device) {
        telemetry = new ScooterTelemetry();
        manager.connect(device.getAddress(), deviceCallback);
    }

    public void registerCallback(ScooterCallback callback) {
        this.callback = callback;
    }

    public void disconnect() {
        manager.disconnect();
    }

    public void setGear(int gear) {
        queueCommand(GEAR, (byte) gear);
    }

    // [FA AF A5] [ZT] [CMD] [LEN] [DATA...] [CHECKSUM]
    private byte[] buildOutgoingPacket(byte zt, byte cmd, byte[] data) {
        int dataLen = (data != null) ? data.length : 0;
        if (dataLen > (255 - 7)) {
            throw new IllegalArgumentException("Too much data: " + dataLen);
        }
        byte[] packet = new byte[4 + 1 + 1 + dataLen + 1];
        packet[0] = (byte) 0xFA;
        packet[1] = (byte) 0xAF;
        packet[2] = (byte) 0xA5;
        packet[3] = zt;
        packet[4] = cmd;
        packet[5] = (byte) dataLen;
        if (data != null) System.arraycopy(data, 0, packet, 6, dataLen);

        int checksum = (zt & 0xFF) + (cmd & 0xFF) + (dataLen & 0xFF);
        for (int i = 0; i < dataLen; i++) checksum += (data[i] & 0xFF);
        packet[packet.length - 1] = (byte) checksum;

        return packet;
    }

    public void queueCommand(byte cmd, byte... data) {
        byte zt = 0x5a;
        manager.queueCommand(io, buildOutgoingPacket(zt, cmd, data));
    }

    // [ZT] [CMD] [LEN] [DATA...] [CHECKSUM] [STATUS]
    private void parseIncomingPacket(byte[] raw) {
        /*if (raw.length >= 1) {
            var zt = ZT.getAndSet(raw[0] & 0xff);
            if (zt == -1) {
                callback.onFirstPacketReceived();
            }
        }*/
        if (raw.length < 5) return;

        int cmd = raw[1] & 0xFF;
        int len = raw[2] & 0xFF;
        int expectedTotal = 3 + len + 2;
        if (raw.length < expectedTotal) return;

        int calc = 0;
        for (int i = 2; i < raw.length; i++)
            calc += (raw[i - 2] & 0xFF);
        calc &= 0xFF;
        int recvChecksum = raw[3 + len] & 0xFF;
        int status = raw[3 + len + 1] & 0xFF;

        byte[] data = new byte[len];
        System.arraycopy(raw, 3, data, 0, len);

        onNotify(cmd, data, status, calc == recvChecksum);
    }

    private void onNotify(int cmd, byte[] data, int status, boolean isValid) {
        switch (cmd) {
            case 0x10 -> parse0x10_Telemetry(data);
            case 0x11 -> parse0x11_State(data);
            case 0x12 -> parse0x12_Versions(data);
            case 0x13 -> parse0x13_Sensors(data);
            case 0x14 -> { /* TODO */ }
            case 0x16 -> parse0x16_BatterySettings(data);
            case MAX_SPEED -> parseMaxSpeed(data);
            case STARTING_TORQUE -> parseStartingTorque(data);
            case MAX_CURRENT -> parseMaxCurrent(data);
            case BRAKE_STRENGTH -> parseBrakeStrength(data);
        }
    }

    private void parse0x10_Telemetry(byte[] d) {
        if (d.length < 12) return;

        telemetry.voltage = (((d[0] & 0xFF) << 8) | (d[1] & 0xFF)) / 100f;
        telemetry.current = (((d[2] & 0xFF) << 8) | (d[3] & 0xFF)) / 1000f;
        telemetry.speed = (((d[4] & 0xFF) << 8) | (d[5] & 0xFF)) / 10f;
        telemetry.batteryPercent = d[6] & 0xFF;
        telemetry.controllerTemp = d[7] & 0xFF;
        telemetry.motorTemp = d[8] & 0xFF;
        telemetry.batteryTemp = d[9] & 0xFF;
        telemetry.motorRpm = ((d[10] & 0xFF) << 8) | (d[11] & 0xFF);

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parse0x11_State(byte[] d) {
        if (d.length < 10) return;

        int flags1 = d[0] & 0xFF;
        telemetry.gear = flags1 & 0x07;
        telemetry.lights = (flags1 & 0x08) != 0;
        telemetry.taillight = (flags1 & 0x10) != 0;
        telemetry.startingMode = (flags1 & 0x20) != 0;
        telemetry.cruiseControl = (flags1 & 0x40) != 0;
        telemetry.unitMph = (flags1 & 0x80) != 0;

        int flags2 = d[1] & 0xFF;
        telemetry.switchControl = (flags2 & 0x01) != 0;
        telemetry.locked = (flags2 & 0x02) != 0;
        telemetry.horn = (flags2 & 0x04) != 0;
        telemetry.leftTurn = (flags2 & 0x08) != 0;
        telemetry.rightTurn = (flags2 & 0x10) != 0;
        telemetry.ambientLight = (flags2 & 0x20) != 0;
        telemetry.bluetoothBound = (flags2 & 0x40) != 0;
        telemetry.reserved_byte_1_bit_7 = (flags2 & 0x80) != 0;

        telemetry.singleMileage = (((d[2] & 0xFF) << 8) | (d[3] & 0xFF)) / 100f;
        telemetry.totalMileage = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);
        telemetry.lightValue = ((d[6] & 0xFF) << 8) | (d[7] & 0xFF);

        int flags3 = d[8] & 0xFF;
        telemetry.cruisingCondition = (flags3 & 0x01) != 0;
        telemetry.brakingState = (flags3 & 0x02) != 0;
        telemetry.lockCondition = (flags3 & 0x04) != 0;
        telemetry.abnormalCommunication = (flags3 & 0x08) != 0;
        telemetry.batteryOvervoltage = (flags3 & 0x10) != 0;
        telemetry.batteryUndervoltage = (flags3 & 0x20) != 0;
        telemetry.motorPhaseFault = (flags3 & 0x40) != 0;
        telemetry.chargingState = (flags3 & 0x80) != 0;

        int flags4 = d[9] & 0xFF;
        telemetry.lockedRotorFault = (flags4 & 0x01) != 0;
        telemetry.hardwareOvercurrent = (flags4 & 0x02) != 0;
        telemetry.controllerFailure = (flags4 & 0x04) != 0;
        telemetry.throttleFault = (flags4 & 0x08) != 0;
        telemetry.brakeSensorFault = (flags4 & 0x10) != 0;
        telemetry.motorHalfFault = (flags4 & 0x20) != 0;
        telemetry.reserved_byte_9_bit_6 = (flags4 & 0x40) != 0;
        telemetry.reserved_byte_9_bit_7 = (flags4 & 0x80) != 0;

        callback.onTelemetryUpdate(telemetry.toState());

        // Синхронизируем состояние: самокат привязан на парковке и отвязан на свободе
        if (!telemetry.locked && telemetry.bluetoothBound) {
            queueCommand(BINDING, (byte) 0x02);
        } else if (telemetry.locked && !telemetry.bluetoothBound) {
            queueCommand(BINDING, (byte) 0x01);
        }
    }

    private void parse0x12_Versions(byte[] d) {
        if (d.length < 9) return;

        telemetry.instrumentMCUID = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
        telemetry.instrumentHwVersion = d[2] & 0xFF;
        telemetry.instrumentSwVersion = d[3] & 0xFF;

        telemetry.controllerMCUID = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);
        telemetry.controllerHwVersion = d[6] & 0xFF;
        telemetry.controllerSwVersion = d[7] & 0xFF;

        telemetry.gearsBitmask = d[8] & 0xFF;

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parse0x13_Sensors(byte[] d) {
        if (d.length < 12) return;

        telemetry.throttleValue = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
        telemetry.brakeVal1 = ((d[2] & 0xFF) << 8) | (d[3] & 0xFF);
        telemetry.brakeVal2 = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);
        telemetry.throttleVal_CY = ((d[6] & 0xFF) << 8) | (d[7] & 0xFF);
        telemetry.brakeVal1_CY = ((d[8] & 0xFF) << 8) | (d[9] & 0xFF);
        telemetry.brakeVal2_CY = ((d[10] & 0xFF) << 8) | (d[11] & 0xFF);

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parse0x16_BatterySettings(byte[] d) {
        if (d.length < 6) return;

        telemetry.batteryCutoffVoltage = d[0] & 0xFF;
        telemetry.batteryLevel1Voltage = d[1] & 0xFF;
        telemetry.batteryLevel2Voltage = d[2] & 0xFF;
        telemetry.batteryLevel3Voltage = d[3] & 0xFF;
        telemetry.batteryLevel4Voltage = d[4] & 0xFF;
        telemetry.batteryLevel5Voltage = d[5] & 0xFF;

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parseMaxSpeed(byte[] d) {
        if (d.length < 2) return;

        telemetry.maxSpeed = d[0] & 0xFF;
        telemetry.maxSpeedLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parseStartingTorque(byte[] d) {
        if (d.length < 2) return;

        telemetry.startTorque = d[0] & 0xFF;
        telemetry.startTorqueLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parseMaxCurrent(byte[] d) {
        if (d.length < 2) return;

        telemetry.maxCurrent = d[0] & 0xFF;
        telemetry.maxCurrentLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private void parseBrakeStrength(byte[] d) {
        if (d.length < 2) return;

        telemetry.brakeStrength = d[0] & 0xFF;
        telemetry.brakeStrengthLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry.toState());
    }

    private final DeviceCallback deviceCallback = new DeviceCallback() {
        @Override
        public void onConnecting() {
            telemetry.connection = CONNECTING;
            callback.onTelemetryUpdate(telemetry.toState());
        }

        @Override
        public void onConnected() {
            telemetry.connection = CONNECTED;
            callback.onTelemetryUpdate(telemetry.toState());
        }

        @Override
        public void onDisconnected() {
            telemetry.connection = DISCONNECTED;
            callback.onTelemetryUpdate(telemetry.toState());
        }

        @Override
        public void onFailedConnection() {
            // TODO
        }

        @Override
        public void onFailedNotifications() {
            onFailedConnection();
            disconnect();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt) {
            var service = gatt.getService(SERVICE_UUID);
            if (service == null) {
                onFailedNotifications();
                return;
            }
            var ch = service.getCharacteristic(IO_CHAR_UUID);
            if (ch == null) {
                onFailedNotifications();
                return;
            }
            if (!manager.enableNotifications(ch, true)) {
                onFailedNotifications();
                return;
            }
            io = ch;

            Log.e("TEST", "CONNECTED");
            queueCommand(START_TELEMETRY);
        }

        @Override
        public void onNotify(BluetoothGattCharacteristic ch, byte[] raw) {
            Log.e("NOTIFY", Arrays.toString(raw));
            parseIncomingPacket(raw);
        }

        @Override
        public void onWrite(BluetoothGattCharacteristic ch, byte[] raw, boolean success) {
            Log.e("WRITE", Arrays.toString(raw));
            // nop
        }
    };
}
