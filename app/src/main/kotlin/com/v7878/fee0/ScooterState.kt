package com.v7878.fee0

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScooterState(
    val connected: Boolean = false,

    val voltage: Float? = null,
    val current: Float? = null,
    val speed: Float? = null,
    val motorRpm: Int? = null,
    val gear: Int? = null,
    val unitMph: Boolean = false,

    val singleMileage: Float? = null,
    val totalMileage: Int? = null,

    val controllerTemp: Int? = null,
    val motorTemp: Int? = null,
    val batteryTemp: Int? = null,

    val throttleValue: Int? = null,
    val brakeVal1: Int? = null,
    val brakeVal2: Int? = null,

    // lights
    // startingMode
    // cruiseControl
    // speedUnitMph
    // locked
    // bluetoothBound
    // cruisingCondition
    // int instrumentVersion, instrumentHwVersion, instrumentSwVersion;
    // int controllerVersion, controllerHwVersion, controllerSwVersion;
    // int throttleValue, brakeVal1, brakeVal2;
    // int maxSpeed, maxSpeedLimit;
    // int startTorque, startTorqueLimit;
    // int maxTorque, maxTorqueLimit;
    // int brakeStrength, brakeStrengthLimit;

    // boolean lockedRotorFault, hardwareOvercurrent, controllerFailure;
    // boolean throttleFault, brakeSensorFault, motorHalfFault;
    // boolean abnormalCommunication, batteryOvervoltage, batteryUndervoltage;
    // boolean motorPhaseFault;
) : Parcelable
