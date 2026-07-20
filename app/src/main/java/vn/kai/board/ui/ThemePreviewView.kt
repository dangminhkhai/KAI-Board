package vn.kai.board.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import vn.kai.board.settings.KeyboardThemePalette

class ThemePreviewView(
    context: Context,
    private val palette: KeyboardThemePalette,
    private val previewHeightDp: Int = 225,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (previewHeightDp * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(palette.background)
        val contentWidth = 400f * density
        val contentHeight = 225f * density
        val scale = minOf(width / contentWidth, height / contentHeight)
        canvas.save()
        canvas.translate((width - contentWidth * scale) / 2f, (height - contentHeight * scale) / 2f)
        canvas.scale(scale, scale)
        val gap = 5f * density
        val margin = 8f * density
        val suggestionHeight = 34f * density
        val suggestionWidth = (contentWidth - margin * 2 - gap * 2) / 3f
        repeat(3) { index ->
            drawKey(canvas, RectF(margin + index * (suggestionWidth + gap), margin, margin + index * (suggestionWidth + gap) + suggestionWidth, margin + suggestionHeight), palette.key, listOf("Xin", "chào", "bạn")[index], 14f)
        }
        val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        val rowHeight = 38f * density
        rows.forEachIndexed { row, labels ->
            val inset = if (row == 1) 12f * density else if (row == 2) 27f * density else 0f
            val top = margin + suggestionHeight + gap + row * (rowHeight + gap)
            val keyWidth = (contentWidth - (margin + inset) * 2 - gap * (labels.length - 1)) / labels.length
            labels.forEachIndexed { index, label ->
                val left = margin + inset + index * (keyWidth + gap)
                drawKey(canvas, RectF(left, top, left + keyWidth, top + rowHeight), palette.key, label.toString(), 13f)
            }
        }
        val bottom = margin + suggestionHeight + gap + 3 * (rowHeight + gap)
        drawKey(canvas, RectF(margin, bottom, contentWidth * .22f, bottom + rowHeight), palette.specialKey, "123", 12f)
        drawKey(canvas, RectF(contentWidth * .24f, bottom, contentWidth * .76f, bottom + rowHeight), palette.key, "KAI Board", 12f)
        drawKey(canvas, RectF(contentWidth * .78f, bottom, contentWidth - margin, bottom + rowHeight), palette.accent, "↵", 16f)
        canvas.restore()
    }

    private fun drawKey(canvas: Canvas, rect: RectF, color: Int, label: String, size: Float) {
        paint.color = color
        canvas.drawRoundRect(rect, 8f * density, 8f * density, paint)
        paint.color = palette.text; paint.textAlign = Paint.Align.CENTER; paint.textSize = size * density
        paint.typeface = android.graphics.Typeface.create(palette.fontFamily, android.graphics.Typeface.NORMAL)
        canvas.drawText(label, rect.centerX(), rect.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
    }
}
