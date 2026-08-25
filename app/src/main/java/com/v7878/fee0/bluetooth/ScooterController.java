package com.v7878.fee0.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.v7878.fee0.ScooterState;

import java.util.UUID;

public class ScooterController {
    public interface ScooterCallback {
        void onTelemetryUpdate(ScooterState telemetry);
    }

    public static final UUID SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID IO_CHAR_UUID = UUID.fromString("0000fee2-0000-1000-8000-00805f9b34fb");

    public static final int MODE_ECO = 1;
    public static final int MODE_D = 2;
    public static final int MODE_S = 3;
    public static final int MODE_WALK = 4;

    private ScooterCallback callback;

    public void connect(BluetoothDevice device) {
        callback.onTelemetryUpdate(new ScooterState(
                true,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    }

    public void registerCallback(ScooterCallback callback) {
        this.callback = callback;
    }

    public void disconnect() {
        callback.onTelemetryUpdate(new ScooterState(
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    }
}
