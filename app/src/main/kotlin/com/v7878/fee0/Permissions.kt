package com.v7878.fee0

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.core.content.ContextCompat

object Permissions {
    fun requiredPermissions(): Array<String> {
        return if (SDK_INT >= VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }
    }

    fun optionalPermissions(): Array<String> {
        return if (SDK_INT >= VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    fun allPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        permissions.addAll(requiredPermissions())
        permissions.addAll(optionalPermissions())
        return permissions.toTypedArray()
    }

    fun allRequiredGranted(context: Context): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED
        }
    }

    fun allGranted(context: Context): Boolean {
        return allPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PERMISSION_GRANTED
        }
    }
}
