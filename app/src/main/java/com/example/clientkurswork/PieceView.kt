package com.example.clientkurswork

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class PieceView(context: Context) : View(context) {
    var color: Int = Color.RED
    var platerId: Int = 0
    var cellId: Int = 0
    var pieceX: Float = 0f
    var pieceY: Float = 0f

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = color
        canvas.drawCircle(pieceX, pieceY, 30f, paint)
    }
}