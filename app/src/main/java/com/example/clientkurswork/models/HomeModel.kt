package com.example.clientkurswork.models

import com.example.clientkurswork.enums.Player

data class HomeModel(
    val id: Int,
    val player: Player,
    val pieces: MutableList<PieceModel>,
    val availForMove: Boolean = false,
)
