package com.example.clientkurswork

import androidx.compose.ui.graphics.Color

data class Piece(val playerId: Int, val pieceId: Int)

enum class FieldType {
    START,
    NORMAL,
    HOME_TRACK,
    FINISH
}

data class Field(val id: Int, val type: FieldType, val isSafe: Boolean = false) {
    var pieces: MutableList<Piece> = mutableListOf()
}

data class Base(val color: Color, val x: Float, val y:Float, val radius: Float)