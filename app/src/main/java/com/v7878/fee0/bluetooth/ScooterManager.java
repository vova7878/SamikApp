package com.v7878.fee0.bluetooth;

import java.util.UUID;

public class ScooterManager {
    public static final UUID SERVICE_UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    private static final UUID IO_CHAR_UUID = UUID.fromString("0000fee2-0000-1000-8000-00805f9b34fb");

    public static final int MODE_ECO = 1;
    public static final int MODE_D = 2;
    public static final int MODE_S = 3;
    public static final int MODE_WALK = 4;
}
