package com.v7878.samik;

import static com.v7878.samik.bluetooth.ScooterManager.RIDE_MODE_D;
import static com.v7878.samik.bluetooth.ScooterManager.RIDE_MODE_ECO;
import static com.v7878.samik.bluetooth.ScooterManager.RIDE_MODE_S;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.v7878.samik.bluetooth.BLEDeviceManager;
import com.v7878.samik.bluetooth.ScooterManager;
import com.v7878.samik.bluetooth.ScooterManager.ScooterCallback;
import com.v7878.samik.bluetooth.ScooterManager.ScooterTelemetry;

@SuppressLint({"DefaultLocale", "SetTextI18n"})
public class MainActivity extends AppCompatActivity {
    private BLEDeviceManager manager;
    private ScooterManager smanager;
    private String deviceAddress;

    // Локальное состояние UI
    private boolean lightsOn = false;
    private boolean parkingOn = false;
    private boolean startingModeOn = false;
    private boolean cruiseOn = false;
    private int currentMode = 0;
    private boolean mphMode = false;

    // Views — header
    private TextView tvDeviceName, tvConnectionStatus;

    // Views — телеметрия
    private TextView tvSpeed, tvSpeedUnit, tvBatteryPercent, tvVoltage, tvCurrent, tvMode;
    private TextView tvRpm, tvCtrlTemp, tvCharging;
    private TextView tvSingleMileage, tvTotalMileage;

    // Views — флаги
    private TextView tvFlagLights, tvFlagLock, tvFlagStart, tvFlagCruise, tvFlagUnits, tvFlagBound;

    // Views — управление
    private Button btnDisconnect;
    private Button btnLights, btnParking;
    private Button btnStartingMode, btnCruise;
    private Button btnToggleUnits, btnHorn;
    private Button btnModeEco, btnModeD, btnModeS;

    // Views — DIY
    private SeekBar sbMaxSpeed, sbStartTorque, sbMaxTorque, sbBrake;
    private TextView tvMaxSpeedValue, tvStartTorqueValue, tvMaxTorqueValue, tvBrakeValue;

    // Views — диагностика
    private TextView tvFirmware;
    private View cardErrors;
    private TextView tvErrors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot), (v, insets) -> {
            var navbar = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, navbar.top, 0, navbar.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Получаем адрес устройства из Intent
        Intent intent = getIntent();
        deviceAddress = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        String deviceName = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME);

        if (deviceAddress == null) {
            // Если адрес не передан — возвращаемся к списку
            startActivity(new Intent(this, DeviceListActivity.class));
            finish();
            return;
        }

        bindViews();
        setupListeners();

        // Имя устройства в заголовке
        tvDeviceName.setText(deviceName != null ? deviceName : "SAMIK");

        initManager();
    }

    private void bindViews() {
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);

        tvSpeed = findViewById(R.id.tvSpeed);
        tvSpeedUnit = findViewById(R.id.tvSpeedUnit);
        tvBatteryPercent = findViewById(R.id.tvBatteryPercent);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvMode = findViewById(R.id.tvMode);

        tvRpm = findViewById(R.id.tvRpm);
        tvCtrlTemp = findViewById(R.id.tvCtrlTemp);
        tvCharging = findViewById(R.id.tvCharging);

        tvSingleMileage = findViewById(R.id.tvSingleMileage);
        tvTotalMileage = findViewById(R.id.tvTotalMileage);

        tvFlagLights = findViewById(R.id.tvFlagLights);
        tvFlagLock = findViewById(R.id.tvFlagLock);
        tvFlagStart = findViewById(R.id.tvFlagStart);
        tvFlagCruise = findViewById(R.id.tvFlagCruise);
        tvFlagUnits = findViewById(R.id.tvFlagUnits);
        tvFlagBound = findViewById(R.id.tvFlagBound);

        btnModeEco = findViewById(R.id.btnModeEco);
        btnModeD = findViewById(R.id.btnModeD);
        btnModeS = findViewById(R.id.btnModeS);

        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnLights = findViewById(R.id.btnLights);
        btnParking = findViewById(R.id.btnParking);
        btnStartingMode = findViewById(R.id.btnStartingMode);
        btnCruise = findViewById(R.id.btnCruise);
        btnToggleUnits = findViewById(R.id.btnToggleUnits);
        btnHorn = findViewById(R.id.btnHorn);

        sbMaxSpeed = findViewById(R.id.sbMaxSpeed);
        sbStartTorque = findViewById(R.id.sbStartTorque);
        sbMaxTorque = findViewById(R.id.sbMaxTorque);
        sbBrake = findViewById(R.id.sbBrake);
        tvMaxSpeedValue = findViewById(R.id.tvMaxSpeedValue);
        tvStartTorqueValue = findViewById(R.id.tvStartTorqueValue);
        tvMaxTorqueValue = findViewById(R.id.tvMaxTorqueValue);
        tvBrakeValue = findViewById(R.id.tvBrakeValue);

        tvFirmware = findViewById(R.id.tvFirmware);
        cardErrors = findViewById(R.id.cardErrors);
        tvErrors = findViewById(R.id.tvErrors);
    }

    private void setupListeners() {
        btnDisconnect.setOnClickListener(v -> {
            if (manager != null) manager.disconnect();
            startActivity(new Intent(this, DeviceListActivity.class));
            finish();
        });

        btnLights.setOnClickListener(v -> {
            smanager.setLights(!lightsOn);
        });

        btnParking.setOnClickListener(v -> {
            smanager.setParkingMode(!parkingOn);
        });

        btnStartingMode.setOnClickListener(v -> {
            smanager.setStartingMode(!startingModeOn);
        });

        btnCruise.setOnClickListener(v -> {
            smanager.setCruiseControl(!cruiseOn);
        });

        btnToggleUnits.setOnClickListener(v -> {
            smanager.setUnits(!mphMode);
        });

        btnHorn.setOnClickListener(v -> {
            smanager.setHorn(true);
        });

        btnModeEco.setOnClickListener(v -> smanager.setRideMode(RIDE_MODE_ECO));
        btnModeD.setOnClickListener(v -> smanager.setRideMode(RIDE_MODE_D));
        btnModeS.setOnClickListener(v -> smanager.setRideMode(RIDE_MODE_S));

        sbMaxSpeed.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvMaxSpeedValue.setText(String.valueOf(p));
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                smanager.setMaxSpeed(sb.getProgress());
            }
        });
        sbStartTorque.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvStartTorqueValue.setText(String.valueOf(p));
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                var p = sb.getProgress() * 200 / sb.getMax();
                smanager.setStartingTorque(p);
            }
        });
        sbMaxTorque.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvMaxTorqueValue.setText(String.valueOf(p));
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                var p = sb.getProgress() * 200 / sb.getMax();
                smanager.setMaxTorque(p);
            }
        });
        sbBrake.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                tvBrakeValue.setText(String.valueOf(p));
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                var p = sb.getProgress() * 200 / sb.getMax();
                smanager.setBrakeStrength(p);
            }
        });
    }

    private void initManager() {
        manager = new BLEDeviceManager(this, smanager = new ScooterManager(new ScooterCallback() {
            @Override
            public void onConnecting() {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("● Подключение...");
                    tvConnectionStatus.setTextColor(0xFF64B5F6);
                });
            }

            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("● Подключено");
                    tvConnectionStatus.setTextColor(0xFF81C784);
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("● Отключено");
                    tvConnectionStatus.setTextColor(0xFFE57373);
                    tvSpeed.setText("0.0");
                });
                startActivity(new Intent(MainActivity.this, DeviceListActivity.class));
                finish();
            }

            @Override
            public void onTelemetryUpdate(ScooterTelemetry t) {
                runOnUiThread(() -> renderTelemetry(t));
            }
        }));

        // Подключаемся к выбранному устройству напрямую
        manager.connect(deviceAddress);
    }

    private void renderTelemetry(ScooterTelemetry t) {
        mphMode = t.speedUnitMph;
        float speedDisplay = mphMode ? t.speed * 0.621371f : t.speed;
        tvSpeed.setText(String.format("%.1f", speedDisplay));
        tvSpeedUnit.setText(mphMode ? " mph" : " km/h");
        tvFlagUnits.setText(mphMode ? "📏 mph" : "📏 km/h");

        // TODO
        //noinspection ConstantValue
        if (false) {
            class Holder {
                static final BatteryPercentageCalculator BPC =
                        new BatteryPercentageCalculator();
            }
            var bp = Holder.BPC.calculate(t.voltage, t.current);
            if (bp >= 0) {
                tvBatteryPercent.setText(bp + "%");
                int color = bp > 50 ? 0xFF81C784 :
                        bp > 20 ? 0xFFFFB74D : 0xFFE57373;
                tvBatteryPercent.setTextColor(color);
            }
        } else {
            if (t.batteryPercent >= 0) {
                tvBatteryPercent.setText(t.batteryPercent + "%");
                int color = t.batteryPercent > 50 ? 0xFF81C784 :
                        t.batteryPercent > 20 ? 0xFFFFB74D : 0xFFE57373;
                tvBatteryPercent.setTextColor(color);
            }
        }

        if (t.voltage > 0) {
            tvVoltage.setText(String.format("%.2f V", t.voltage));
        }
        tvCurrent.setText(String.format("%.2f A", t.current));

        var gearName = switch (t.gear) {
            case 1 -> "ECO";
            case 2 -> "D";
            case 3 -> "S";
            default -> "?";
        };
        var gearColor = switch (t.gear) {
            case 1 -> 0xFF81C784;
            case 2 -> 0xFFFFB74D;
            case 3 -> 0xFFE57373;
            default -> 0xFFCE93D8;
        };

        tvMode.setText(gearName);
        tvMode.setTextColor(gearColor);
        currentMode = t.gear;
        updateModeButtons();

        tvRpm.setText(t.motorRpm + " rpm");
        tvCtrlTemp.setText(t.controllerTemp + "°C");
        tvCtrlTemp.setTextColor(t.controllerTemp > 60 ? 0xFFE57373 : 0xFFFFB74D);
        tvCharging.setText(t.chargingState ? "ДА" : "НЕТ");
        tvCharging.setTextColor(t.chargingState ? 0xFF81C784 : 0xFF9E9E9E);

        tvSingleMileage.setText(String.format("%.2f km", t.singleMileage));
        if (t.totalMileage > 0) {
            tvTotalMileage.setText(t.totalMileage + " km");
        }

        tvFlagLights.setText("💡 Свет: " + (t.lights ? "ВКЛ" : "ВЫКЛ"));
        tvFlagLights.setTextColor(t.lights ? 0xFFFFD54F : 0xFFFFFFFF);
        tvFlagLock.setText("🔒 Парковка: " + (t.locked ? "ВКЛ" : "ВЫКЛ"));
        tvFlagLock.setTextColor(t.locked ? 0xFFE57373 : 0xFFFFFFFF);
        tvFlagStart.setText("🎯 Кик-старт: " + (t.startingMode ? "ВКЛ" : "ВЫКЛ"));
        tvFlagCruise.setText("⛵ Круиз: " + (t.cruiseControl ? "ВКЛ" : "ВЫКЛ"));
        tvFlagBound.setText("📡 Привязан: " + (t.bluetoothBound ? "ДА" : "НЕТ"));

        if (t.lights != lightsOn) {
            lightsOn = t.lights;
            updateLightsButton();
        }
        if (t.locked != parkingOn) {
            parkingOn = t.locked;
            updateParkingButton();
        }
        if (t.startingMode != startingModeOn) {
            startingModeOn = t.startingMode;
            updateStartingModeButton();
        }
        if (t.cruiseControl != cruiseOn) {
            cruiseOn = t.cruiseControl;
            updateCruiseButton();
        }

        if (t.maxSpeedLimit != 0) {
            sbMaxSpeed.setProgress(t.maxSpeed);
            sbMaxSpeed.setMax(t.maxSpeedLimit);
        }
        if (t.startTorqueLimit != 0) {
            sbStartTorque.setProgress(t.startTorque);
            sbStartTorque.setMax(t.startTorqueLimit);
        }
        if (t.maxTorqueLimit != 0) {
            sbMaxTorque.setProgress(t.maxTorque);
            sbMaxTorque.setMax(t.maxTorqueLimit);
        }
        if (t.brakeStrengthLimit != 0) {
            sbBrake.setProgress(t.brakeStrength);
            sbBrake.setMax(t.brakeStrengthLimit);
        }

        if (t.instrumentVersion != 0 || t.controllerVersion != 0) {
            tvFirmware.setText(String.format(
                    "Прошивка: IV:0x%04X | IHW:%d | ISW:%d | CV:0x%04X | CHW:%d | CSW:%d",
                    t.instrumentVersion, t.instrumentHwVersion, t.instrumentSwVersion,
                    t.controllerVersion, t.controllerHwVersion, t.controllerSwVersion));
        }

        if (t.hasErrors()) {
            StringBuilder err = new StringBuilder();
            if (t.lockedRotorFault) err.append("• Блокировка ротора\n");
            if (t.hardwareOvercurrent) err.append("• Перегрузка по току\n");
            if (t.controllerFailure) err.append("• Ошибка контроллера\n");
            if (t.throttleFault) err.append("• Ошибка ручки газа\n");
            if (t.brakeSensorFault) err.append("• Ошибка датчика тормоза\n");
            if (t.motorHalfFault) err.append("• Ошибка мотора\n");
            if (t.motorPhaseFault) err.append("• Ошибка фаз мотора\n");
            if (t.batteryOvervoltage) err.append("• Перенапряжение батареи\n");
            if (t.batteryUndervoltage) err.append("• Низкое напряжение батареи\n");
            if (t.abnormalCommunication) err.append("• Ошибка связи\n");

            tvErrors.setText(err.toString().trim());
            cardErrors.setVisibility(View.VISIBLE);
        } else {
            cardErrors.setVisibility(View.GONE);
        }
    }

    private void updateLightsButton() {
        btnLights.setText(lightsOn ? "💡 Свет: ВКЛ" : "💡 Свет: ВЫКЛ");
        btnLights.setTextColor(lightsOn ? Color.BLACK : Color.WHITE);
        btnLights.setBackgroundResource(lightsOn ?
                R.drawable.btn_yellow : R.drawable.btn_non_active);
    }

    private void updateParkingButton() {
        btnParking.setText(parkingOn ? "🔒 Снять парковку" : "🔓 Парковка");
        btnParking.setBackgroundResource(parkingOn ?
                R.drawable.btn_red : R.drawable.btn_non_active);
    }

    private void updateStartingModeButton() {
        btnStartingMode.setText(startingModeOn ? "🎯 Кик-старт: ВКЛ" : "🎯 Кик-старт: ВЫКЛ");
        btnStartingMode.setTextColor(startingModeOn ? Color.BLACK : Color.WHITE);
        btnStartingMode.setBackgroundResource(startingModeOn ?
                R.drawable.btn_active : R.drawable.btn_non_active);
    }

    private void updateCruiseButton() {
        btnCruise.setText(cruiseOn ? "⛵ Круиз: ВКЛ" : "⛵ Круиз: ВЫКЛ");
        btnCruise.setTextColor(cruiseOn ? Color.BLACK : Color.WHITE);
        btnCruise.setBackgroundResource(cruiseOn ?
                R.drawable.btn_active : R.drawable.btn_non_active);
    }

    private void updateModeButtons() {
        btnModeEco.setBackgroundResource(R.drawable.btn_non_active);
        btnModeD.setBackgroundResource(R.drawable.btn_non_active);
        btnModeS.setBackgroundResource(R.drawable.btn_non_active);

        switch (currentMode) {
            case RIDE_MODE_ECO -> btnModeEco.setBackgroundResource(R.drawable.btn_active);
            case RIDE_MODE_D -> btnModeD.setBackgroundResource(R.drawable.btn_active);
            case RIDE_MODE_S -> btnModeS.setBackgroundResource(R.drawable.btn_active);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manager != null) {
            manager.disconnect();
            manager = null;
        }
    }

    private interface SimpleSeekBarListener extends SeekBar.OnSeekBarChangeListener {
        @Override
        default void onStartTrackingTouch(SeekBar sb) {
        }
    }
}
