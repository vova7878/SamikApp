package com.v7878.fee0

class ScooterState {
    var connected: Boolean by mainThreadStateOf(false)

    var name: String? by mainThreadStateOf(null)
    var voltage: Float? by mainThreadStateOf(null)
    var current: Float? by mainThreadStateOf(null)
    var speed: Float? by mainThreadStateOf(null)
    var motorRpm: Int? by mainThreadStateOf(null)
    var gear: Int? by mainThreadStateOf(null)
    var unitMph: Boolean by mainThreadStateOf(false)

    var singleMileage: Float? by mainThreadStateOf(null)
    var totalMileage: Int? by mainThreadStateOf(null)

    var controllerTemp: Int? by mainThreadStateOf(null)
    var motorTemp: Int? by mainThreadStateOf(null)
    var batteryTemp: Int? by mainThreadStateOf(null)

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
}
