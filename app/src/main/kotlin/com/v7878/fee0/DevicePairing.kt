package com.v7878.fee0

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult

class DevicePairing(
    private val activity: ComponentActivity,
    private val onDevicePaired: (BluetoothDevice) -> Unit,
    private val onPairingFailed: (String?) -> Unit
) {
    private val intentSenderLauncher = activity.registerForActivityResult(
        StartIntentSenderForResult()
    ) { result ->
        handleResult(result.resultCode, result.data)
    }

    fun startPairing() {
        val scanFilter = ScanFilter.Builder()
            //TODO? .setServiceUuid(ParcelUuid(ScooterManager.SERVICE_UUID))
            .build()

        val leFilter = BluetoothLeDeviceFilter.Builder()
            .setScanFilter(scanFilter)
            .build()

        val request = AssociationRequest.Builder()
            .addDeviceFilter(leFilter)
            .setSingleDevice(false)
            .build()

        val deviceManager = activity.getSystemService(CompanionDeviceManager::class.java)

        if (SDK_INT >= VERSION_CODES.TIRAMISU) {
            deviceManager.associate(
                request,
                activity.mainExecutor,
                object : CompanionDeviceManager.Callback() {
                    override fun onAssociationPending(intentSender: IntentSender) {
                        launchIntentSender(intentSender)
                    }

                    override fun onAssociationCreated(associationInfo: AssociationInfo) {
                        if (SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val device = associationInfo.associatedDevice?.bleDevice?.device
                            if (device != null) {
                                onDevicePaired.invoke(device)
                            }
                        }
                    }

                    override fun onFailure(errorMessage: CharSequence?) {
                        onPairingFailed.invoke(errorMessage?.toString())
                    }
                }
            )
        } else {
            deviceManager.associate(
                request,
                object : CompanionDeviceManager.Callback() {
                    @Deprecated("Deprecated in Java")
                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        launchIntentSender(chooserLauncher)
                    }

                    override fun onFailure(error: CharSequence?) {
                        onPairingFailed.invoke(error?.toString())
                    }
                },
                null
            )
        }
    }

    private fun launchIntentSender(intentSender: IntentSender) {
        val request = IntentSenderRequest.Builder(intentSender).build()
        intentSenderLauncher.launch(request)
    }

    @Suppress("DEPRECATION")
    private fun handleResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (SDK_INT < VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val scanResult: ScanResult? = if (SDK_INT >= VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra(
                    CompanionDeviceManager.EXTRA_DEVICE,
                    ScanResult::class.java
                )
            } else {
                data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
            }
            val device = scanResult?.device

            if (device != null) {
                onDevicePaired.invoke(device)
            }
        }
    }
}
