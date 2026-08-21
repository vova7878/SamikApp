package com.v7878.fee0.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v7878.fee0.ui.theme.BgCard
import com.v7878.fee0.ui.theme.lighten

@Composable
fun SimpleCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    color: Color = BgCard,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color,
        border = BorderStroke(1.dp, color.lighten(0.15f))
    ) {
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}
