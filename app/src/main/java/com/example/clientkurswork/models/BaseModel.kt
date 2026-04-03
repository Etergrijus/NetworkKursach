package com.example.clientkurswork.models

import com.example.clientkurswork.enums.Player

data class BaseModel(
    val player: Player,
    //val pieceCount: Int,
    val pieces: MutableList<PieceModel>,
)
