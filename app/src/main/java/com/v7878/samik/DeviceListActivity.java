package com.v7878.samik;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DeviceListActivity extends AppCompatActivity {
    private static final int REQ_PERMS = 100;
    private static final long SCAN_TIMEOUT_MS = 15000;
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String EXTRA_DEVICE_NAME = "device_name";

    private static final String[] PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PERMISSIONS = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            PERMISSIONS = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };
        }
    }

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;

    private RecyclerView rvDevices;
    private TextView tvScanStatus;
    private TextView tvEmpty;
    private Button btnScan;

    private DeviceAdapter adapter;
    private final List<DeviceItem> devices = new ArrayList<>();
    private boolean isScanning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scanTimeoutRunnable = this::stopScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deviceListRoot), (v, insets) -> {
            var navbar = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, navbar.top, 0, navbar.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }
        if (bluetoothAdapter != null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        rvDevices = findViewById(R.id.rvDevices);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnScan = findViewById(R.id.btnScan);

        adapter = new DeviceAdapter(devices, device -> {
            // Пользователь выбрал устройство — открываем MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, device.address);
            intent.putExtra(EXTRA_DEVICE_NAME, device.name);
            startActivity(intent);
            finish();
        });

        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(adapter);

        btnScan.setOnClickListener(v -> {
            if (isScanning) {
                stopScan();
            } else {
                if (hasAllPermissions()) {
                    startScan();
                } else {
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_PERMS);
                }
            }
        });

        updateEmptyState();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startScan() {
        if (scanner == null || bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            toast("Bluetooth недоступен или выключен");
            return;
        }
        if (isScanning) return;

        devices.clear();
        adapter.notifyDataSetChanged();
        updateEmptyState();

        isScanning = true;
        tvScanStatus.setText("● Сканирование...");
        tvScanStatus.setTextColor(0xFFFFB74D);
        btnScan.setText("⏹ ОСТАНОВИТЬ");

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();

        try {
            scanner.startScan(null, settings, scanCallback);
        } catch (SecurityException e) {
            toast("Ошибка доступа: " + e.getMessage());
            stopScan();
            return;
        }

        handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (!isScanning) return;
        isScanning = false;

        handler.removeCallbacks(scanTimeoutRunnable);

        try {
            if (scanner != null) scanner.stopScan(scanCallback);
        } catch (Exception ignored) {
        }

        tvScanStatus.setText("● Готово");
        tvScanStatus.setTextColor(0xFF81C784);
        btnScan.setText("🔍 СКАНИРОВАТЬ");
        updateEmptyState();
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                BluetoothDevice device = result.getDevice();
                String name = device.getName();
                if (name == null) return;

                String address = device.getAddress();
                int rssi = result.getRssi();

                // Проверяем, нет ли уже такого устройства
                for (DeviceItem item : devices) {
                    if (item.address.equals(address)) {
                        // Обновляем RSSI если стал лучше
                        if (rssi > item.rssi) {
                            item.rssi = rssi;
                            int idx = devices.indexOf(item);
                            runOnUiThread(() -> adapter.notifyItemChanged(idx));
                        }
                        return;
                    }
                }

                // Добавляем новое устройство
                DeviceItem newItem = new DeviceItem(name, address, rssi);
                runOnUiThread(() -> {
                    devices.add(newItem);
                    adapter.notifyItemInserted(devices.size() - 1);
                    updateEmptyState();
                });
            } catch (SecurityException e) {
                // Игнорируем
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                toast("Ошибка сканирования: " + errorCode);
                stopScan();
            });
        }
    };

    private void updateEmptyState() {
        if (devices.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvDevices.setVisibility(View.GONE);
            if (!isScanning) {
                tvEmpty.setText("Нажмите кнопку для поиска самокатов");
            } else {
                tvEmpty.setText("Поиск самокатов...");
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvDevices.setVisibility(View.VISIBLE);
        }
    }

    private boolean hasAllPermissions() {
        for (String p : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(requestCode, perms, res);
        if (requestCode == REQ_PERMS) {
            if (hasAllPermissions()) {
                startScan();
            } else {
                toast("Разрешения не предоставлены");
            }
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    public static class DeviceItem {
        public final String name;
        public final String address;
        public int rssi;

        public DeviceItem(String name, String address, int rssi) {
            this.name = name;
            this.address = address;
            this.rssi = rssi;
        }
    }
}