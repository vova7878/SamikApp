package com.v7878.fee0

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.v7878.fee0.bluetooth.ScooterController
import com.v7878.fee0.bluetooth.ScooterController.ScooterCallback

class ScooterService : Service() {
    private lateinit var controller: ScooterController

    inner class ScooterBinder : Binder() {
        fun connect(device: BluetoothDevice) {
            controller.connect(device)
        }

        fun disconnect() {
            controller.disconnect()
        }

        fun registerCallback(callback: ScooterCallback) {
            controller.registerCallback(callback)
        }

        fun promoteToForeground() {
            this@ScooterService.promoteToForeground()
        }

        fun demoteToBackground() {
            this@ScooterService.demoteToBackground()
        }

        fun setMode(mode: Int) {
            TODO()
        }
    }

    private val binder = ScooterBinder()

    companion object {
        const val CHANNEL_ID = "scooter_telemetry"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        controller = ScooterController()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun promoteToForeground() {
        // TODO открытие приложения при нажатии
        // TODO кнопка отключения
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_message))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (SDK_INT >= VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                0
            },
        )
    }

    fun demoteToBackground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        //controller.stop()
        super.onDestroy()
    }
}
