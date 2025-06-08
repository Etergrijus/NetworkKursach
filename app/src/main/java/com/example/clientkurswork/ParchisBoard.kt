package com.example.clientkurswork

class ParchisBoard {
    private val safeZones: List<Int> = listOf(5, 12, 17, 22, 29, 34, 39, 46, 51, 56, 63, 68)

    private val startFields: List<Int> = listOf(5, 22, 39, 56)

    val fields: List<Field> = createBoard()

    private fun createBoard(): List<Field> {
        val board = mutableListOf<Field>()

        for (i in 1..68) {
            val type = if (i in startFields)
                FieldType.START
            else
                FieldType.NORMAL
            val isSafe = i in safeZones
            board.add(Field(i, type, isSafe))
        }

        return board
    }

    fun movePiece(piece: Piece, fromFieldId: Int, toFieldId: Int) {
        val fromField = fields.find { it.id == fromFieldId }
        val toField = fields.find { it.id == toFieldId }

        if (fromField != null && toField != null) {
            fromField.pieces.remove(piece)
            toField.pieces.add(piece)
        }
    }
}