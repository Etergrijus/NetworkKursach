package com.example.clientkurswork

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ParchisBoardUI(viewModel: ParchisViewModel, imageViewWidth: Int, imageViewHeight: Int) {
    val boardState = viewModel.board.value
    val fields = boardState.fields
    val context = LocalContext.current

    // Создаем фишки для каждого игрока
    val redPieces = List(4) { Piece(1, it + 1) }
    val yellowPieces = List(4) { Piece(2, it + 1) }
    val bluePieces = List(4) { Piece(3, it + 1) }
    val greenPieces = List(4) { Piece(4, it + 1) }

    // Размещаем фишки на стартовых позициях
    redPieces.forEach { piece -> boardState.fields.find { it.id == 5 }?.pieces?.add(piece) }
    yellowPieces.forEach { piece -> boardState.fields.find { it.id == 22 }?.pieces?.add(piece) }
    bluePieces.forEach { piece -> boardState.fields.find { it.id == 39 }?.pieces?.add(piece) }
    greenPieces.forEach { piece -> boardState.fields.find { it.id == 56 }?.pieces?.add(piece) }

    for (field in fields) {
        for (piece in field.pieces) {
            val coordinates = getCoordinatesForCell(field.id, imageViewWidth, imageViewHeight)
            Log.d("ParchisBoardUI", "Field ID: $field.id, Coordinates: (${coordinates.first}, ${coordinates.second})")
            Canvas(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = coordinates.first - 25f // Центрируем фишку
                        translationY = coordinates.second - 25f // Центрируем фишку
                    }
            ) {
                drawCircle(
                    color = when (piece.playerId) {
                        1 -> Color.Red
                        2 -> Color.Yellow
                        3 -> Color.Blue
                        4 -> Color.Green
                        else -> Color.Black
                    },
                    radius = 25f
                )
            }
        }
    }
}

// Функция для получения координат ячейки
fun getCoordinatesForCell(cellId: Int, imageViewWidth: Int, imageViewHeight: Int): Pair<Float, Float> {
    val cellWidth = imageViewWidth / 1f
    val cellHeight = imageViewHeight / 1f

    val col = (cellId - 1) % 15
    val row = (cellId - 1) / 15

    val x = col * cellWidth + cellWidth / 5
    val y = row * cellHeight + cellHeight / 5

    return Pair(x, y)
}



