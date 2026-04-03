package com.example.clientkurswork.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.clientkurswork.enums.Player
import com.example.clientkurswork.models.BaseModel
import com.example.clientkurswork.models.PieceModel
import com.example.clientkurswork.models.PlayerModel

@Composable
fun PieceView(
    piece: PieceModel,
    modifier: Modifier = Modifier,
    onClick: (PieceModel?) -> Unit,
    currentPlayer: PlayerModel?
) {
    val color = when (piece.player) {
        Player.YELLOW -> Color(0xffffbf00)
        Player.BLUE -> Color(0xff42aaff)
        Player.RED -> Color(0xffff4c5b)
        Player.GREEN -> Color(0xff4cbb17)
    }

    Canvas(Modifier
        .clickable(
            //Выключаем визуальный эффект нажатия
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            if (currentPlayer != null) {
                if (piece.player == currentPlayer.onBoardMatching)
                    onClick(piece)
                else
                    onClick(null)
            }
        }
        .then(modifier)) {
        val borderWidth = 2.dp.toPx()
        val radius = 30f - borderWidth
        drawCircle(
            color = color, radius = radius
        )
        //Контур
        drawCircle(
            color = Color.Black,
            radius = radius,
            style = Stroke(width = borderWidth)
        )
    }
}