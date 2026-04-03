package com.example.clientkurswork.models

import com.example.clientkurswork.enums.CellType
import com.example.clientkurswork.enums.Player

data class CellModel(
    val ordinalNumber: Int,
    val type: CellType,
    val pieces: MutableList<PieceModel>,
    val player: Player? = null,
    val availForMove: Boolean = false,
)
