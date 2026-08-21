package com.v7878.fee0.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v7878.fee0.ui.theme.BtnBlue
import com.v7878.fee0.ui.theme.BtnDarkBlue
import com.v7878.fee0.ui.theme.desaturate
import com.v7878.fee0.ui.theme.lighten

interface ButtonState {
    val containerColor: Color
    val hasBorder: Boolean
}

enum class SimpleButtonState(
    override val containerColor: Color,
    override val hasBorder: Boolean
) : ButtonState {
    Active(
        containerColor = BtnBlue,
        hasBorder = false
    ),
    NonActive(
        containerColor = BtnDarkBlue,
        hasBorder = true
    )
}

fun simpleState(state: Boolean): SimpleButtonState {
    return when (state) {
        true -> SimpleButtonState.Active
        false -> SimpleButtonState.NonActive
    }
}

@Composable
fun <BS : ButtonState> StatefulButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: BS,
    content: @Composable () -> Unit,
) {
    val containerColor = state.containerColor
    val disabledContainerColor = containerColor.desaturate(0.5f)

    val borderColor = if (enabled) {
        containerColor.lighten(0.15f)
    } else {
        disabledContainerColor.lighten(0.1f)
    }

    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = disabledContainerColor,
            contentColor = Color.White
        ),
        border = if (!state.hasBorder) null else BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

// @Composable
// fun SimpleButton(
//     onClick: () -> Unit,
//     modifier: Modifier = Modifier,
//     enabled: Boolean = true,
//     color: Color = BtnBlue,
//     content: @Composable () -> Unit,
// ) {
//     StatefulButton(
//         onClick = onClick,
//         modifier = modifier,
//         enabled = enabled,
//         state = TODO;
//     ) {
//         content()
//     }
// }
