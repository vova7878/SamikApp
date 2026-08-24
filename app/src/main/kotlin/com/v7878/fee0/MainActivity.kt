package com.v7878.fee0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.v7878.fee0.bluetooth.ScooterManager.MODE_D
import com.v7878.fee0.bluetooth.ScooterManager.MODE_ECO
import com.v7878.fee0.bluetooth.ScooterManager.MODE_S
import com.v7878.fee0.bluetooth.ScooterManager.MODE_WALK
import com.v7878.fee0.ui.ButtonState
import com.v7878.fee0.ui.SimpleCard
import com.v7878.fee0.ui.StatefulButton
import com.v7878.fee0.ui.simpleState
import com.v7878.fee0.ui.theme.ActivityBackground
import com.v7878.fee0.ui.theme.Batt0
import com.v7878.fee0.ui.theme.Batt100
import com.v7878.fee0.ui.theme.Batt50
import com.v7878.fee0.ui.theme.Batt90
import com.v7878.fee0.ui.theme.BgCardText
import com.v7878.fee0.ui.theme.BtnBlue
import com.v7878.fee0.ui.theme.BtnRed
import com.v7878.fee0.ui.theme.DividerColor
import com.v7878.fee0.ui.theme.IndicatorBG
import com.v7878.fee0.ui.theme.MainTheme
import com.v7878.fee0.ui.theme.White

class MainActivity : ComponentActivity() {
    private val scooterState = ScooterState()
    //private val controller = ScooterController(scooterState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainTheme(darkTheme = true) {
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier
                            .background(ActivityBackground)
                            .padding(innerPadding)
                    ) {
                        Main(state = scooterState)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Main(state = ScooterState())
}

@Composable
fun NamedElement(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    innerPadding: Dp = 10.dp,
    name: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = name,
            color = BgCardText,
            fontSize = 11.sp,
            letterSpacing = 0.1.em
        )
        Spacer(Modifier.height(innerPadding))
        content()
    }
}

@Composable
fun TextElement(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    innerPadding: Dp = 10.dp,
    name: String,
    value: String
) {
    NamedElement(
        modifier = modifier,
        alignment = alignment,
        innerPadding = innerPadding,
        name = name
    ) {
        Text(
            text = value,
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class ConnectionButtonState(
    override val containerColor: Color,
    override val hasBorder: Boolean
) : ButtonState {
    Connected(
        containerColor = BtnRed,
        hasBorder = false
    ),
    Disconnected(
        containerColor = BtnBlue,
        hasBorder = false
    )
}

@Composable
fun Main(state: ScooterState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(state = state)
        SimpleCard(
            contentPadding = PaddingValues(20.dp)
        ) {
            DashboardCard(state = state)
        }
        SimpleCard {
            ModeSelection(state = state)
        }
        SimpleCard {
            MileageBlock(state = state)
        }
        SimpleCard {
            TemperatureBlock(state = state)
        }
    }
}

@Composable
fun Header(state: ScooterState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = state.name ?: stringResource(R.string.device_not_selected),
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        StatefulButton(
            state = when (state.connected) {
                true -> ConnectionButtonState.Connected
                false -> ConnectionButtonState.Disconnected
            },
            onClick = {
                state.connected = !state.connected
            },
            modifier = Modifier
                .widthIn(min = 150.dp)
        ) {
            Text(
                text = when (state.connected) {
                    true -> stringResource(R.string.disconnect)
                    false -> stringResource(R.string.connect)
                }
            )
        }
    }
}

@Composable
fun VerticalIndicator(
    value: Int?,
    maxValue: Int = 250,
    backgroundColor: Color = IndicatorBG,
    valueColor: Color = BtnBlue,
    width: Dp = 16.dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
    ) {
        val fillHeight = when (value) {
            null -> 0f
            else -> (value.toFloat() / maxValue).coerceIn(0f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = fillHeight)
                .align(Alignment.BottomCenter)
                .background(valueColor)
        )
    }
}

@Composable
fun DashboardCard(state: ScooterState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VerticalIndicator(value = state.brakeVal1, valueColor = BtnRed)
            VerticalIndicator(value = state.brakeVal2, valueColor = BtnRed)
            NamedElement(
                name = stringResource(R.string.speed),
                alignment = Alignment.Start,
                innerPadding = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (val speed = state.speed) {
                            null -> stringResource(R.string.speed_placeholder)
                            else -> stringResource(R.string.speedometer, speed)
                        },
                        color = White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        // TODO: локализация
                        text = if (state.unitMph) "mph" else "km/h",
                        color = BgCardText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NamedElement(
                name = stringResource(R.string.rpm),
                alignment = Alignment.End,
                innerPadding = 8.dp
            ) {
                Text(
                    text = state.motorRpm?.toString() ?: "---",
                    color = White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            VerticalIndicator(value = state.throttleValue, valueColor = BtnBlue)
        }
    }
}

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

@Composable
fun ModeSelection(state: ScooterState) {
    NamedElement(
        name = stringResource(R.string.mode_selection),
        alignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatefulButton(
                state = simpleState(state.gear == MODE_ECO),
                onClick = { state.gear = MODE_ECO },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Text(
                    text = "ECO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            StatefulButton(
                state = simpleState(state.gear == MODE_D),
                onClick = { state.gear = MODE_D },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Text(
                    text = "D",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            StatefulButton(
                state = simpleState(state.gear == MODE_S),
                onClick = { state.gear = MODE_S },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Text(
                    text = "S",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            StatefulButton(
                state = simpleState(state.gear == MODE_WALK),
                onClick = { state.gear = MODE_WALK },
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Text(
                    text = "WALK",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MileageBlock(state: ScooterState) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
    ) {
        TextElement(
            modifier = Modifier.weight(1f),
            alignment = Alignment.Start,
            name = stringResource(R.string.single_mileage),
            value = when (val mileage = state.singleMileage) {
                null -> stringResource(R.string.mileage_placeholder)
                // TODO: мили
                else -> stringResource(R.string.single_mileage_km, mileage)
            }
        )
        VerticalDivider(color = DividerColor)
        TextElement(
            modifier = Modifier.weight(1f),
            alignment = Alignment.End,
            name = stringResource(R.string.total_mileage),
            value = when (val mileage = state.totalMileage) {
                null -> stringResource(R.string.mileage_placeholder)
                // TODO: мили
                else -> stringResource(R.string.total_mileage_km, mileage)
            }
        )
    }
}

@Composable
fun TemperatureBlock(state: ScooterState) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
    ) {
        TextElement(
            modifier = Modifier.weight(1f),
            name = stringResource(R.string.controller),
            value = when (val temp = state.controllerTemp) {
                null -> stringResource(R.string.temp_placeholder)
                else -> stringResource(R.string.temp_celsius, temp)
            }
        )
        VerticalDivider(color = DividerColor)
        TextElement(
            modifier = Modifier.weight(1f),
            name = stringResource(R.string.motor),
            value = when (val temp = state.motorTemp) {
                null -> stringResource(R.string.temp_placeholder)
                else -> stringResource(R.string.temp_celsius, temp)
            }
        )
        VerticalDivider(color = DividerColor)
        TextElement(
            modifier = Modifier.weight(1f),
            name = stringResource(R.string.battery),
            value = when (val temp = state.batteryTemp) {
                null -> stringResource(R.string.temp_placeholder)
                else -> stringResource(R.string.temp_celsius, temp)
            }
        )
    }
}
