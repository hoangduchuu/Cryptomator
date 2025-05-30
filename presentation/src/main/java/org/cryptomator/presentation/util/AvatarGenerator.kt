package org.cryptomator.presentation.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvatarGenerator @Inject constructor() {

    fun createLetterAvatar(firstLetter: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Generate consistent color based on first letter
        val hash = firstLetter.hashCode()
        val r = (hash and 0xFF0000) shr 16
        val g = (hash and 0x00FF00) shr 8
        val b = hash and 0x0000FF
        val randomColor = Color.rgb(r, g, b)

        // Draw circle background
        val paint = Paint().apply {
            color = randomColor
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Draw text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 72f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        // Calculate text position to be centered
        val textBounds = Rect()
        textPaint.getTextBounds(firstLetter, 0, firstLetter.length, textBounds)
        val x = size / 2f
        val y = size / 2f + (textBounds.height() / 2f) - textBounds.bottom
        
        canvas.drawText(firstLetter, x, y, textPaint)
        
        return bitmap
    }
} 