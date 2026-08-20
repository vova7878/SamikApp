package com.v7878.fee0

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v7878.fee0.ui.theme.BtnActive
import com.v7878.fee0.ui.theme.BtnNonActive
import com.v7878.fee0.ui.theme.desaturate
import com.v7878.fee0.ui.theme.lighten

@Composable
fun SimpleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = BtnActive,
    text: String
) {
    val disabledColor = color.desaturate(0.5f)

    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = disabledColor,
            contentColor = Color.White
        ),
        border = null
        //contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = text)
    }
}

@Composable
fun StatefulButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = BtnActive,
    nonActiveColor: Color = BtnNonActive,
    text: String,
    active: Boolean
) {
    val containerColor = if (active) activeColor else nonActiveColor
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
        border = if (active) null else BorderStroke(1.dp, borderColor)
        //contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = text)
    }
}