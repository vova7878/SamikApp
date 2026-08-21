package com.v7878.fee0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7878.fee0.bluetooth.ScooterManager.MODE_D
import com.v7878.fee0.bluetooth.ScooterManager.MODE_ECO
import com.v7878.fee0.bluetooth.ScooterManager.MODE_S
import com.v7878.fee0.bluetooth.ScooterManager.MODE_WALK
import com.v7878.fee0.ui.theme.ActivityBackground
import com.v7878.fee0.ui.theme.BgCard
import com.v7878.fee0.ui.theme.BgCardBorder
import com.v7878.fee0.ui.theme.BtnBlue
import com.v7878.fee0.ui.theme.BtnRed
import com.v7878.fee0.ui.theme.MainTheme

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
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = BorderStroke(1.dp, BgCardBorder)
    ) {
        content()
    }
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
                text = state.name ?: "??_????????",
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
                    text = if (state.connected)
                        stringResource(R.string.disconnect)
                    else
                        stringResource(R.string.connect)
                )
            }
        }
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatefulButton(
                    state = simpleState(state.gear == MODE_ECO),
                    onClick = { state.gear = MODE_ECO },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
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
                        .weight(1f)
                        .heightIn(min = 52.dp)
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
                        .weight(1f)
                        .heightIn(min = 52.dp)
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
                        .weight(1f)
                        .heightIn(min = 52.dp)
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
}
