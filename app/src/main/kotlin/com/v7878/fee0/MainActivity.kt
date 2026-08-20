package com.v7878.fee0

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.v7878.fee0.ui.theme.ActivityBackground
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

@Composable
fun Main(state: ScooterState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        StatefulButton(
            text = "Нажми меня",
            active = true,
            onClick = { }
        )
        SimpleButton(
            text = "Нажми меня",
            color = BtnRed,
            onClick = { }
        )
    }
}
