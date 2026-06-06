package com.v7878.samik.bluetooth;

import static com.v7878.samik.bluetooth.ScooterManager.Commands.BINDING;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.BRAKE_STRENGTH;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.CRUISE_CONTROL;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.HEARTBEAT;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.HORN;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.LIGHT;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.MAX_SPEED;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.MAX_TORQUE;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.MODE_SWITCH;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.PARKING_MODE;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.STARTING_MODE;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.STARTING_TORQUE;
import static com.v7878.samik.bluetooth.ScooterManager.Commands.UNITS;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.v7878.samik.bluetooth.BLEDeviceManager.DeviceCallback;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScooterManager implements DeviceCallback {
    public static final String TAG = "ScooterBLE";

    private static final int HEARTBEAT_INTERVAL_MS = 3000;

    public static class Commands {
        public static final byte HEARTBEAT = (byte) 0x01;
        public static final byte PARKING_MODE = (byte) 0x33;
        public static final byte HORN = (byte) 0x34;
        public static final byte STARTING_MODE = (byte) 0x35;
        public static final byte CRUISE_CONTROL = (byte) 0x36;
        public static final byte MAX_SPEED = (byte) 0x3C;
        public static final byte STARTING_TORQUE = (byte) 0x3D;
        public static final byte MAX_TORQUE = (byte) 0x3E;
        public static final byte BRAKE_STRENGTH = (byte) 0x3F;
        public static final byte MODE_SWITCH = (byte) 0x42;
        public static final byte UNITS = (byte) 0x43;
        public static final byte LIGHT = (byte) 0x45;
        public static final byte BINDING = (byte) 0x4C;
    }

    public void setLights(boolean enable) {
        manager.queueCommand(LIGHT, enable ? (byte) 0x02 : (byte) 0x01);
    }

    public void setParkingMode(boolean enable) {
        manager.queueCommand(PARKING_MODE, enable ? (byte) 0x02 : (byte) 0x01);
    }

    public void setStartingMode(boolean enable) {
        manager.queueCommand(STARTING_MODE, enable ? (byte) 0x02 : (byte) 0x01);
    }

    public void setCruiseControl(boolean enable) {
        manager.queueCommand(CRUISE_CONTROL, enable ? (byte) 0x02 : (byte) 0x01);
    }

    public void setUnits(boolean mph) {
        manager.queueCommand(UNITS, mph ? (byte) 0x02 : (byte) 0x01);
    }

    public void setHorn(boolean enable) {
        manager.queueCommand(HORN, enable ? (byte) 0x01 : (byte) 0x00);
    }

    public static final int RIDE_MODE_ECO = 1;
    public static final int RIDE_MODE_D = 2;
    public static final int RIDE_MODE_S = 3;

    public void setRideMode(int mode) {
        manager.queueCommand(MODE_SWITCH, (byte) mode);
    }

    public void setMaxSpeed(int value) {
        manager.queueCommand(MAX_SPEED, (byte) value);
    }

    public void setStartingTorque(int value) {
        manager.queueCommand(STARTING_TORQUE, (byte) value);
    }

    public void setMaxTorque(int value) {
        manager.queueCommand(MAX_TORQUE, (byte) value);
    }

    public void setBrakeStrength(int value) {
        manager.queueCommand(BRAKE_STRENGTH, (byte) value);
    }

    public static class ScooterTelemetry {
        // 0x10
        public float voltage, current, speed;
        public int batteryPercent, controllerTemp, motorTemp, batteryTemp, motorRpm;

        // 0x11
        public int gear;
        public boolean lights, taillight, startingMode, cruiseControl, speedUnitMph;
        public boolean switchControl, locked, horn, leftTurn, rightTurn, ambientLight, bluetoothBound;
        public float singleMileage;
        public int totalMileage, ambientLightValue;

        public boolean cruisingCondition, brakingState, lockCondition;
        public boolean abnormalCommunication, batteryOvervoltage, batteryUndervoltage;
        public boolean motorPhaseFault;

        public boolean chargingState;

        public boolean lockedRotorFault, hardwareOvercurrent, controllerFailure;
        public boolean throttleFault, brakeSensorFault, motorHalfFault;

        // 0x12
        public int instrumentVersion, instrumentHwVersion, instrumentSwVersion;
        public int controllerVersion, controllerHwVersion, controllerSwVersion;
        public int gearsBitmask;

        // 0x13
        public int throttleValue, brakeVal1, brakeVal2;
        public int throttleVal_CY, brakeVal1_CY, brakeVal2_CY;

        // 0x3c - 0x3f
        public int maxSpeed, maxSpeedLimit;
        public int startTorque, startTorqueLimit;
        public int maxTorque, maxTorqueLimit;
        public int brakeStrength, brakeStrengthLimit;

        public boolean hasErrors() {
            return lockedRotorFault || hardwareOvercurrent || controllerFailure
                    || throttleFault || brakeSensorFault || motorHalfFault
                    || motorPhaseFault || batteryOvervoltage || batteryUndervoltage
                    || abnormalCommunication;
        }
    }

    public interface ScooterCallback {
        void onConnecting();

        void onConnected();

        void onDisconnected();

        void onTelemetryUpdate(ScooterTelemetry telemetry);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!connected.get()) return;

            manager.queueCommand(HEARTBEAT);
            mainHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
        }
    };

    private BLEDeviceManager manager;
    private final ScooterCallback callback;
    private final ScooterTelemetry telemetry;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    public ScooterManager(ScooterCallback callback) {
        this.callback = Objects.requireNonNull(callback);
        this.telemetry = new ScooterTelemetry();
    }

    @Override
    public void onAttach(BLEDeviceManager manager) {
        this.manager = Objects.requireNonNull(manager);
    }

    @Override
    public void onConnecting() {
        callback.onConnecting();
    }

    @Override
    public void onConnected() {
        connected.set(true);
        callback.onConnected();
        mainHandler.post(heartbeatRunnable);

        // Опрос текущего состояния
        manager.queueCommand(MAX_SPEED, (byte) 0x00);
        manager.queueCommand(STARTING_TORQUE, (byte) 0x00);
        manager.queueCommand(MAX_TORQUE, (byte) 0x00);
        manager.queueCommand(BRAKE_STRENGTH, (byte) 0x00);
    }

    @Override
    public void onDisconnected() {
        connected.set(false);
        callback.onDisconnected();
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

        callback.onTelemetryUpdate(telemetry);
    }

    private void parse0x11_State(byte[] d) {
        if (d.length < 10) return;

        int flags1 = d[0] & 0xFF;
        int flags2 = d[1] & 0xFF;
        int flags3 = d[8] & 0xFF;
        int flags4 = d[9] & 0xFF;

        telemetry.gear = flags1 & 0x07;
        telemetry.lights = (flags1 & 0x08) != 0;
        telemetry.taillight = (flags1 & 0x10) != 0;
        telemetry.startingMode = (flags1 & 0x20) != 0;
        telemetry.cruiseControl = (flags1 & 0x40) != 0;
        telemetry.speedUnitMph = (flags1 & 0x80) != 0;

        telemetry.switchControl = (flags2 & 0x01) != 0;
        telemetry.locked = (flags2 & 0x02) != 0;
        telemetry.horn = (flags2 & 0x04) != 0;
        telemetry.leftTurn = (flags2 & 0x08) != 0;
        telemetry.rightTurn = (flags2 & 0x10) != 0;
        telemetry.ambientLight = (flags2 & 0x20) != 0;
        telemetry.bluetoothBound = (flags2 & 0x40) != 0;

        telemetry.singleMileage = (((d[2] & 0xFF) << 8) | (d[3] & 0xFF)) / 100f;
        telemetry.totalMileage = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);
        telemetry.ambientLightValue = ((d[6] & 0xFF) << 8) | (d[7] & 0xFF);

        telemetry.cruisingCondition = (flags3 & 0x01) != 0;
        telemetry.brakingState = (flags3 & 0x02) != 0;
        telemetry.lockCondition = (flags3 & 0x04) != 0;
        telemetry.abnormalCommunication = (flags3 & 0x08) != 0;
        telemetry.batteryOvervoltage = (flags3 & 0x10) != 0;
        telemetry.batteryUndervoltage = (flags3 & 0x20) != 0;
        telemetry.motorPhaseFault = (flags3 & 0x40) != 0;
        telemetry.chargingState = (flags3 & 0x80) != 0;

        telemetry.lockedRotorFault = (flags4 & 0x01) != 0;
        telemetry.hardwareOvercurrent = (flags4 & 0x02) != 0;
        telemetry.controllerFailure = (flags4 & 0x04) != 0;
        telemetry.throttleFault = (flags4 & 0x08) != 0;
        telemetry.brakeSensorFault = (flags4 & 0x10) != 0;
        telemetry.motorHalfFault = (flags4 & 0x20) != 0;

        callback.onTelemetryUpdate(telemetry);

        if (!telemetry.locked && telemetry.bluetoothBound) {
            manager.queueCommand(BINDING, (byte) 0x02);
        } else if (telemetry.locked && !telemetry.bluetoothBound) {
            manager.queueCommand(BINDING, (byte) 0x01);
        }
    }

    private void parse0x12_Versions(byte[] d) {
        if (d.length < 9) return;

        telemetry.instrumentVersion = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
        telemetry.instrumentHwVersion = d[2] & 0xFF;
        telemetry.instrumentSwVersion = d[3] & 0xFF;

        telemetry.controllerVersion = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);
        telemetry.controllerHwVersion = d[6] & 0xFF;
        telemetry.controllerSwVersion = d[7] & 0xFF;

        telemetry.gearsBitmask = d[8] & 0xFF;

        callback.onTelemetryUpdate(telemetry);
    }

    private void parse0x13_Sensors(byte[] d) {
        if (d.length < 12) return;

        telemetry.throttleValue = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
        telemetry.brakeVal1 = ((d[2] & 0xFF) << 8) | (d[3] & 0xFF);
        telemetry.brakeVal2 = ((d[4] & 0xFF) << 8) | (d[5] & 0xFF);
        telemetry.throttleVal_CY = ((d[6] & 0xFF) << 8) | (d[7] & 0xFF);
        telemetry.brakeVal1_CY = ((d[8] & 0xFF) << 8) | (d[9] & 0xFF);
        telemetry.brakeVal2_CY = ((d[10] & 0xFF) << 8) | (d[11] & 0xFF);

        callback.onTelemetryUpdate(telemetry);
    }

    private void parseMaxSpeed(byte[] d) {
        if (d.length < 2) return;

        telemetry.maxSpeed = d[0] & 0xFF;
        telemetry.maxSpeedLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry);
    }

    private void parseStartingTorque(byte[] d) {
        if (d.length < 2) return;

        telemetry.startTorque = d[0] & 0xFF;
        telemetry.startTorqueLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry);
    }

    private void parseMaxTorque(byte[] d) {
        if (d.length < 2) return;

        telemetry.maxTorque = d[0] & 0xFF;
        telemetry.maxTorqueLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry);
    }

    private void parseBrakeStrength(byte[] d) {
        if (d.length < 2) return;

        telemetry.brakeStrength = d[0] & 0xFF;
        telemetry.brakeStrengthLimit = d[1] & 0xFF;

        callback.onTelemetryUpdate(telemetry);
    }

    @Override
    public void onNotify(int cmd, byte[] data, int status, boolean isValid) {
        switch (cmd) {
            case 0x10 -> parse0x10_Telemetry(data);
            case 0x11 -> parse0x11_State(data);
            case 0x12 -> parse0x12_Versions(data);
            case 0x13 -> parse0x13_Sensors(data);
            // TODO?
            case 0x14 -> { /* Ignore */ }
            case 0x16 -> { /* Ignore */ }
            case MAX_SPEED -> parseMaxSpeed(data);
            case STARTING_TORQUE -> parseStartingTorque(data);
            case MAX_TORQUE -> parseMaxTorque(data);
            case BRAKE_STRENGTH -> parseBrakeStrength(data);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("cmd: %02X data: ", cmd & 0xFF));
        for (byte b : data) sb.append(String.format("%02X ", b & 0xFF));
        sb.append(String.format("status: %02X", status & 0xFF));
        Log.i(TAG, sb.toString());
    }

    @Override
    public void onRawNotify(byte[] raw) {
    }

    @Override
    public void onRawWrite(byte[] raw, boolean success) {
    }
}
