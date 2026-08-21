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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import com.v7878.fee0.ui.theme.BgCardText
import com.v7878.fee0.ui.theme.BtnBlue
import com.v7878.fee0.ui.theme.BtnRed
import com.v7878.fee0.ui.theme.DividerColor
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
fun TextElement(
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    name: String,
    value: String
) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = name,
            color = BgCardText,
            fontSize = 11.sp,
            letterSpacing = 0.1.em
        )
        Spacer(Modifier.height(10.dp))
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
        SimpleCard {
            Column {
                Text(
                    text = stringResource(R.string.mode_selection),
                    color = BgCardText,
                    fontSize = 11.sp,
                    letterSpacing = 0.1.em
                )
                Spacer(Modifier.height(10.dp))
                ModeSelection(state = state)
            }
        }
        SimpleCard {
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
        SimpleCard {
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
    }
}

@Composable
fun ModeSelection(state: ScooterState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        /*.heightIn(min = 52.dp)*/
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
