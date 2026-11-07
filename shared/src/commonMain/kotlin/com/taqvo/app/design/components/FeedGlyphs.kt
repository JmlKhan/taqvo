package com.taqvo.app.design.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill

/*
 * The feed's own glyphs, on the same 16-unit grid and stroke as Glyphs.kt so they sit beside the
 * share and copy marks as one set.
 */

/** A heart: outlined, or [filled] once the card is liked. */
internal fun DrawScope.drawHeart(tint: Color, filled: Boolean) {
    val u = size.width / 16f
    val heart = Path().apply {
        moveTo(8f * u, 13.6f * u)
        cubicTo(2.4f * u, 9.8f * u, 1.6f * u, 7.2f * u, 2.2f * u, 5.4f * u)
        cubicTo(2.9f * u, 3.2f * u, 6.2f * u, 2.6f * u, 8f * u, 5.2f * u)
        cubicTo(9.8f * u, 2.6f * u, 13.1f * u, 3.2f * u, 13.8f * u, 5.4f * u)
        cubicTo(14.4f * u, 7.2f * u, 13.6f * u, 9.8f * u, 8f * u, 13.6f * u)
        close()
    }
    if (filled) drawPath(heart, tint, style = Fill)
    drawPath(heart, tint, style = glyphStroke())
}

/** A flame, for the streak. */
internal fun DrawScope.drawFlame(tint: Color) {
    val u = size.width / 16f
    val flame = Path().apply {
        moveTo(8f * u, 14.2f * u)
        cubicTo(4.6f * u, 14.2f * u, 3.4f * u, 11.6f * u, 3.8f * u, 9.4f * u)
        cubicTo(4.2f * u, 7.2f * u, 6.4f * u, 5.8f * u, 7.2f * u, 1.8f * u)
        cubicTo(9.4f * u, 3.4f * u, 10.2f * u, 5.6f * u, 9.8f * u, 7.4f * u)
        cubicTo(10.8f * u, 6.8f * u, 11.4f * u, 6f * u, 11.6f * u, 5.2f * u)
        cubicTo(12.8f * u, 6.8f * u, 13f * u, 9f * u, 12.2f * u, 11f * u)
        cubicTo(11.4f * u, 13f * u, 9.8f * u, 14.2f * u, 8f * u, 14.2f * u)
        close()
    }
    drawPath(flame, tint, style = glyphStroke())
}

/** A bell, for the reminders. */
internal fun DrawScope.drawBell(tint: Color) {
    val u = size.width / 16f
    val bell = Path().apply {
        moveTo(3.2f * u, 11.6f * u)
        lineTo(12.8f * u, 11.6f * u)
        cubicTo(11.8f * u, 10.6f * u, 11.6f * u, 9.6f * u, 11.6f * u, 7.6f * u)
        cubicTo(11.6f * u, 5.2f * u, 10f * u, 3.4f * u, 8f * u, 3.4f * u)
        cubicTo(6f * u, 3.4f * u, 4.4f * u, 5.2f * u, 4.4f * u, 7.6f * u)
        cubicTo(4.4f * u, 9.6f * u, 4.2f * u, 10.6f * u, 3.2f * u, 11.6f * u)
        close()
    }
    drawPath(bell, tint, style = glyphStroke())
    drawLine(tint, Offset(6.8f * u, 13.6f * u), Offset(9.2f * u, 13.6f * u), strokeWidth = size.width * 0.0875f)
}

/** "Aa": a large and a small A, for the text style editor. */
internal fun DrawScope.drawTextStyle(tint: Color) {
    val u = size.width / 16f
    val stroke = glyphStroke()
    val big = Path().apply {
        moveTo(1.6f * u, 13.4f * u); lineTo(5.6f * u, 2.8f * u); lineTo(9.6f * u, 13.4f * u)
        moveTo(3.1f * u, 9.6f * u); lineTo(8.1f * u, 9.6f * u)
    }
    val small = Path().apply {
        moveTo(9.4f * u, 13.4f * u); lineTo(11.9f * u, 7f * u); lineTo(14.4f * u, 13.4f * u)
        moveTo(10.3f * u, 11.2f * u); lineTo(13.5f * u, 11.2f * u)
    }
    drawPath(big, tint, style = stroke)
    drawPath(small, tint, style = stroke)
}

/** A downward arrow into a tray: save the image. */
internal fun DrawScope.drawDownload(tint: Color) {
    val u = size.width / 16f
    val path = Path().apply {
        moveTo(8f * u, 2.4f * u); lineTo(8f * u, 10f * u)
        moveTo(4.8f * u, 6.9f * u); lineTo(8f * u, 10f * u); lineTo(11.2f * u, 6.9f * u)
        moveTo(2.6f * u, 10.6f * u); lineTo(2.6f * u, 13.4f * u); lineTo(13.4f * u, 13.4f * u); lineTo(13.4f * u, 10.6f * u)
    }
    drawPath(path, tint, style = glyphStroke())
}

/** An eye, struck through when [struck]: hide or show the watermark. */
internal fun DrawScope.drawEye(tint: Color, struck: Boolean) {
    val u = size.width / 16f
    val eye = Path().apply {
        moveTo(1.6f * u, 8f * u)
        cubicTo(3.4f * u, 4.6f * u, 5.6f * u, 3.6f * u, 8f * u, 3.6f * u)
        cubicTo(10.4f * u, 3.6f * u, 12.6f * u, 4.6f * u, 14.4f * u, 8f * u)
        cubicTo(12.6f * u, 11.4f * u, 10.4f * u, 12.4f * u, 8f * u, 12.4f * u)
        cubicTo(5.6f * u, 12.4f * u, 3.4f * u, 11.4f * u, 1.6f * u, 8f * u)
        close()
    }
    drawPath(eye, tint, style = glyphStroke())
    drawCircle(tint, radius = 1.8f * u, center = Offset(8f * u, 8f * u), style = glyphStroke())
    if (struck) drawLine(tint, Offset(2.6f * u, 13.4f * u), Offset(13.4f * u, 2.6f * u), strokeWidth = size.width * 0.0875f)
}

/** A card behind a card: the feed tab. */
internal fun DrawScope.drawCards(tint: Color) {
    val u = size.width / 16f
    val stroke = glyphStroke()
    val front = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(3.2f * u, 4.2f * u), Size(9.6f * u, 10.2f * u)), 1.6f * u, 1.6f * u)) }
    drawPath(front, tint, style = stroke)
    drawLine(tint, Offset(5f * u, 1.8f * u), Offset(11f * u, 1.8f * u), strokeWidth = size.width * 0.0875f)
    drawCircle(tint, radius = 0.95f * u, center = Offset(8f * u, 8.6f * u))
}

/** A check, for the goal met. */
internal fun DrawScope.drawCheck(tint: Color) {
    val u = size.width / 16f
    val path = Path().apply { moveTo(3f * u, 8.4f * u); lineTo(6.6f * u, 12f * u); lineTo(13f * u, 4.4f * u) }
    drawPath(path, tint, style = glyphStroke())
}

/** A question mark in a ring: the widget guide chip. */
internal fun DrawScope.drawQuestion(tint: Color) {
    val u = size.width / 16f
    drawCircle(tint, radius = 6.4f * u, center = Offset(8f * u, 8f * u), style = glyphStroke())
    val hook = Path().apply {
        moveTo(6f * u, 6.4f * u)
        cubicTo(6f * u, 4.2f * u, 10f * u, 4.2f * u, 10f * u, 6.4f * u)
        cubicTo(10f * u, 7.8f * u, 8f * u, 7.8f * u, 8f * u, 9.4f * u)
    }
    drawPath(hook, tint, style = glyphStroke())
    drawCircle(tint, radius = 0.8f * u, center = Offset(8f * u, 11.8f * u))
}

/** Four small squares: the Ilovalar (tools) button. */
internal fun DrawScope.drawGrid(tint: Color) {
    val u = size.width / 16f
    val stroke = glyphStroke()
    listOf(2.2f to 2.2f, 9.2f to 2.2f, 2.2f to 9.2f, 9.2f to 9.2f).forEach { (x, y) ->
        drawRoundRect(tint, topLeft = Offset(x * u, y * u), size = Size(4.6f * u, 4.6f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.2f * u), style = stroke)
    }
}

/** A framed picture with a hill and a sun: the Mavzular (themes) button. */
internal fun DrawScope.drawPicture(tint: Color) {
    val u = size.width / 16f
    val stroke = glyphStroke()
    drawRoundRect(tint, topLeft = Offset(1.8f * u, 2.8f * u), size = Size(12.4f * u, 10.4f * u), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.6f * u), style = stroke)
    val hill = Path().apply { moveTo(2.4f * u, 11.6f * u); lineTo(6.4f * u, 7.6f * u); lineTo(9.4f * u, 10.4f * u); lineTo(11f * u, 9f * u); lineTo(13.6f * u, 11.4f * u) }
    drawPath(hill, tint, style = stroke)
    drawCircle(tint, radius = 1.2f * u, center = Offset(10.8f * u, 5.8f * u))
}

/** The Kaaba with its band: the Qibla tile. */
internal fun DrawScope.drawKaaba(tint: Color) {
    val u = size.width / 16f
    val stroke = glyphStroke()
    val body = Path().apply {
        moveTo(2.4f * u, 4.6f * u); lineTo(8f * u, 2.6f * u); lineTo(13.6f * u, 4.6f * u)
        lineTo(13.6f * u, 12.8f * u); lineTo(8f * u, 14.6f * u); lineTo(2.4f * u, 12.8f * u); close()
        moveTo(2.4f * u, 4.6f * u); lineTo(8f * u, 6.6f * u); lineTo(13.6f * u, 4.6f * u)
        moveTo(8f * u, 6.6f * u); lineTo(8f * u, 14.6f * u)
    }
    drawPath(body, tint, style = stroke)
    val band = Path().apply { moveTo(2.4f * u, 7f * u); lineTo(8f * u, 9f * u); lineTo(13.6f * u, 7f * u) }
    drawPath(band, tint, style = stroke)
}
