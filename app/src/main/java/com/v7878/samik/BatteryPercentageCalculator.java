package com.v7878.samik;

public class BatteryPercentageCalculator {
    // 📊 Калибровочная таблица для 10S Li-ion (3.0V–4.2V на ячейку)
    // Основана на реальной кривой разряда + ваших порогах P29-P33 и P17=30V
    private static final float[] VOLTAGE_TABLE = {
            30.0f, 33.0f, 34.0f, 35.0f, 36.0f, 37.0f, 38.0f, 39.0f, 40.0f, 41.0f, 42.0f
    };
    private static final int[] PERCENT_TABLE = {
            0, 5, 10, 18, 30, 45, 60, 75, 88, 95, 100
    };

    private float smoothedVoltage = 0f;
    private boolean initialized = false;

    // Коэффициенты сглаживания (EMA-фильтр)
    private static final float ALPHA_REST = 0.25f;  // Быстрое обновление в покое
    private static final float ALPHA_LOAD = 0.04f;  // Жёсткое сглаживание под нагрузкой

    /**
     * Рассчитывает плавный и точный процент заряда
     *
     * @param rawVoltage  Сырое напряжение из пакета 0x10
     * @param currentAmps Ток мотора из пакета 0x10 (для компенсации просадок)
     * @return Процент 0–100
     */
    public int calculate(float rawVoltage, float currentAmps) {
        if (!initialized) {
            smoothedVoltage = rawVoltage;
            initialized = true;
        }

        // Выбираем коэффициент сглаживания в зависимости от нагрузки
        float alpha = Math.abs(currentAmps) > 0.8f ? ALPHA_LOAD : ALPHA_REST;
        smoothedVoltage = alpha * rawVoltage + (1.0f - alpha) * smoothedVoltage;

        // Ограничиваем диапазон калибровки
        float v = Math.max(VOLTAGE_TABLE[0], Math.min(VOLTAGE_TABLE[VOLTAGE_TABLE.length - 1], smoothedVoltage));

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

    /**
     * Сброс фильтра при переподключении
     */
    public void reset() {
        initialized = false;
        smoothedVoltage = 0f;
    }
}
