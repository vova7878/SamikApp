package com.v7878.fee0

class ScooterState {
    var connected: Boolean by mainThreadStateOf(false)

    var name: String? by mainThreadStateOf(null)
    var voltage: Float? by mainThreadStateOf(null)
    var current: Float? by mainThreadStateOf(null)
    var speed: Float? by mainThreadStateOf(null)
    var motorRpm: Int? by mainThreadStateOf(null)
    var gear: Int? by mainThreadStateOf(null)
    // int controllerTemp
    // lights
    // startingMode
    // cruiseControl
    // speedUnitMph
    // locked
    // bluetoothBound
    // float singleMileage
    // int totalMileage
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
}
