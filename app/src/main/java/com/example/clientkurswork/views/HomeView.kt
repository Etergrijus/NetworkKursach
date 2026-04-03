package com.example.clientkurswork.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.HomeModel
import com.example.clientkurswork.models.PieceModel
import com.example.clientkurswork.models.PlayerModel

@Composable
fun HomeView(
    point1: Pair<Float, Float>,
    point2: Pair<Float, Float>,
    home: HomeModel,
) {
    val color = when (home.player) {
        Player.YELLOW -> Color.Yellow
        Player.BLUE -> Color.Blue
        Player.RED -> Color.Red
        Player.GREEN -> Color.Green
    }

    Canvas(Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(size.width / 2, size.height / 2)
            lineTo(point1.first, point1.second)
            lineTo(point2.first, point2.second)
        }
        drawPath(path, color)
    }
}
