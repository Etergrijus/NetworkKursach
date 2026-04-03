package com.example.clientkurswork.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.BaseModel


@Composable
fun BaseView(base: BaseModel) {
    val color = when (base.player) {
        Player.YELLOW -> Color.Yellow
        Player.BLUE -> Color.Blue
        Player.RED -> Color.Red
        Player.GREEN -> Color.Green
    }

    Canvas(Modifier.fillMaxSize()) {
        val borderWidth = 2.dp.toPx()
        drawCircle(
            color = color, radius = (size.height / 2f - borderWidth)
        )
        //Контур
        drawCircle(
            color = Color.Black,
            radius = (size.height / 2f - borderWidth),
            style = Stroke(width = borderWidth)
        )
    }
}