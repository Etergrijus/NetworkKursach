package com.example.clientkurswork.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.clientkurswork.enums.CellTextDirection
import com.example.clientkurswork.enums.CellType
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.CellModel

@Composable
fun CellView(cell: CellModel, cellTextDirection: CellTextDirection? = null,
             onClick: () -> Unit) {
    //Стилизиуем текст, выводимый на ячейке
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = textMeasurer.measure(
        text = cell.ordinalNumber.toString(),
        style = TextStyle(fontSize = 12.sp)
    )

    //Определяем цвет ячейки
    val color = when (cell.type) {
        CellType.PATH -> Color.White
        CellType.SAFE -> Color.Gray
        CellType.START, CellType.HOMESTRETCH -> {
            //Цвет конкретного игрока
            when (cell.player) {
                Player.YELLOW -> Color.Yellow
                Player.BLUE -> Color.Blue
                Player.RED -> Color.Red
                Player.GREEN -> Color.Green
                else -> throw IllegalStateException("Cell rendering error")
            }
        }
    }

    val degree = when (cellTextDirection) {
        CellTextDirection.STRAIGHT, null -> 0f
        CellTextDirection.ON_LEFT_SIDE -> -90f
        CellTextDirection.ON_RIGHT_SIDE -> 90f
    }

    Canvas(
        Modifier.fillMaxSize().clickable(
            //Выключаем визуальный эффект нажатия
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            if (cell.availForMove)
                onClick()
        }
    ) {
        drawRect(
            color = color, size = size
        )

        if (cell.type != CellType.HOMESTRETCH)
            rotate(degrees = degree) {
                drawText(
                    textLayoutResult,
                    topLeft = Offset(
                        x = center.x - textLayoutResult.size.width / 2,
                        y = center.y - textLayoutResult.size.height / 2
                    )
                )
            }
    }
}