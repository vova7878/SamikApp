package com.v7878.samik;

public class BatteryPercentageCalculator {
    // Калибровочная таблица для 10S Li-ion (3.0V–4.2V на ячейку)
    private static final float[] VOLTAGE_TABLE = {
            30.0f, 33.0f, 34.0f, 35.0f, 36.0f, 36.5f, 37.0f, 37.5f,
            38.0f, 38.5f, 39.0f, 40.0f, 40.5f, 41.0f, 41.5f, 42.0f
    };
    private static final int[] PERCENT_TABLE = {
            0, 1, 3, 5, 10, 15, 20, 30, 40, 50, 60, 75, 85, 90, 95, 100
    };

    public static int calculate(float voltage) {
        // Ограничиваем диапазон калибровки
        float v = Math.max(VOLTAGE_TABLE[0], Math.min(VOLTAGE_TABLE[VOLTAGE_TABLE.length - 1], voltage));

        // Кусочно-линейная интерполяция
        for (int i = 0; i < VOLTAGE_TABLE.length - 1; i++) {
            if (v >= VOLTAGE_TABLE[i] && v <= VOLTAGE_TABLE[i + 1]) {
                float vLow = VOLTAGE_TABLE[i];
                float vHigh = VOLTAGE_TABLE[i + 1];
                int pLow = PERCENT_TABLE[i];
                int pHigh = PERCENT_TABLE[i + 1];
                float ratio = (v - vLow) / (vHigh - vLow);
                return Math.round(pLow + ratio * (pHigh - pLow));
            }
        }
        return 0;
    }
}
