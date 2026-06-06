package com.v7878.samik;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

@SuppressLint("SetTextI18n")
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceListActivity.DeviceItem device);
    }

    private final List<DeviceListActivity.DeviceItem> devices;
    private final OnDeviceClickListener listener;

    public DeviceAdapter(List<DeviceListActivity.DeviceItem> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceListActivity.DeviceItem device = devices.get(position);
        holder.tvName.setText(device.name);
        holder.tvAddress.setText(device.address);
        holder.tvRssi.setText(device.rssi + " dBm");
        holder.tvSignalQuality.setText(getSignalQualityText(device.rssi));
        holder.tvSignalQuality.setTextColor(getSignalQualityColor(device.rssi));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDeviceClick(device);
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    private String getSignalQualityText(int rssi) {
        if (rssi >= -50) return "📶 Отличный";
        if (rssi >= -70) return "📶 Хороший";
        if (rssi >= -80) return "📶 Средний";
        return "📶 Слабый";
    }

    private int getSignalQualityColor(int rssi) {
        if (rssi >= -50) return 0xFF81C784; // Зелёный
        if (rssi >= -70) return 0xFFB0BEC5; // Светло-серый
        if (rssi >= -80) return 0xFFFFB74D; // Оранжевый
        return 0xFFE57373; // Красный
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvRssi;
        TextView tvSignalQuality;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvAddress = itemView.findViewById(R.id.tvDeviceAddress);
            tvRssi = itemView.findViewById(R.id.tvRssi);
            tvSignalQuality = itemView.findViewById(R.id.tvSignalQuality);
        }
    }
}