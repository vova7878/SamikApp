package com.v7878.fee0

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MainThreadState<T>(initialValue: T) : ReadWriteProperty<Any?, T> {
    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())
    }

    private val state: MutableState<T> = mutableStateOf(initialValue)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            state.value = value
        } else {
            mainHandler.post { state.value = value }
        }
    }
}

// Функция-конструктор
fun <T> mainThreadStateOf(initialValue: T) = MainThreadState(initialValue)