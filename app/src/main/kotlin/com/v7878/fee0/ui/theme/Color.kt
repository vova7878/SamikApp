package com.v7878.fee0.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

var White = Color(0xFFFFFFFF)

val ActivityBackground = Color(0xFF0B1220)

val BtnBlue = Color(0xFF4E79C4)
val BtnDarkBlue = Color(0xFF293344)
val BtnRed = Color(0xFFC54E58)

val BgCard = Color(0xFF151E2E)
var BgCardText = Color(0xFF6B7A90)

var DividerColor = Color(0xFF2A3447)
var IndicatorBG = DividerColor

var Batt100 = Color(0xFF3B82F6)
var Batt90 = Color(0xFF22C55E)
var Batt50 = Color(0xFFFBBF24)
var Batt0 = Color(0xFFEF4444)

fun batteryColor(percent: Int): Color {
    val p = percent.coerceIn(0, 100)

    val points = listOf(
        100 to Batt100,
        90 to Batt90,
        50 to Batt50,
        0 to Batt0
    )

    for (i in 0 until points.size - 1) {
        val (p1, c1) = points[i]
        val (p2, c2) = points[i + 1]

        if (p in p2..p1) {
            val fraction = (p1 - p).toFloat() / (p1 - p2)
            return lerp(c1, c2, fraction)
        }
    }

    throw AssertionError()
}
