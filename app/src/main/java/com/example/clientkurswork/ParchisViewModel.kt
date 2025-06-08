package com.example.clientkurswork

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class ParchisViewModel : ViewModel() {
    private val _board = mutableStateOf(ParchisBoard())
    val board: State<ParchisBoard> = _board
}