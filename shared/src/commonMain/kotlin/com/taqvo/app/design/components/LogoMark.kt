package com.taqvo.app.design.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tazkir's mark: a crescent with an eight-pointed star, the glyph of the app icon, traced from the
 * icon's own artwork. It lives in a 1024-unit square with the glyph centred, so a caller that wants
 * it [size] wide scales by `size / LOGO_SPACE`.
 */
const val LOGO_SPACE = 1024f

private const val FROM_ICON = 1024f / 432f

// The icon artwork's own 432 px space: the glyph's box is x 115..318, y 100..308.
private fun x(v: Float) = (v - 216.5f) * FROM_ICON + 512f
private fun y(v: Float) = (v - 204f) * FROM_ICON + 512f

private fun circle(cx: Float, cy: Float, r: Float) = Path().apply {
    addOval(androidx.compose.ui.geometry.Rect(Offset(x(cx - r), y(cy - r)), Offset(x(cx + r), y(cy + r))))
}

private val crescent: Path by lazy {
    Path.combine(PathOperation.Difference, circle(218.4f, 201.8f, 104.1f), circle(243.2f, 187.2f, 87.2f))
}

private val star: Path by lazy {
    Path().apply {
        // Eight points, the four on the axes long and the four between them short.
        val cx = 252f
        val cy = 185f
        for (i in 0 until 16) {
            val angle = i * PI.toFloat() / 8 - PI.toFloat() / 2
            val r = when {
                i % 4 == 0 -> 42f
                i % 2 == 0 -> 27f
                else -> 9f
            }
            val px = x(cx + r * cos(angle))
            val py = y(cy + r * sin(angle))
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}

/** The mark in its own [LOGO_SPACE] square at the scope's origin: [crescentColor] and [starColor]. */
fun DrawScope.drawLogoGlyph(crescentColor: Color, starColor: Color) {
    drawPath(crescent, crescentColor)
    drawPath(star, starColor)
}

/** The mark scaled to fill the scope's shorter side. */
fun DrawScope.drawLogo(crescentColor: Color, starColor: Color) {
    val k = size.minDimension / LOGO_SPACE
    withTransform({ scale(k, k, Offset.Zero) }) { drawLogoGlyph(crescentColor, starColor) }
}

/** The mark [height] tall (its box is about 0.48 of the square) with its top edge at [topCentre]. */
fun DrawScope.drawLogoAt(topCentre: Offset, height: Float, crescentColor: Color, starColor: Color) {
    val k = height / (LOGO_SPACE * 0.4875f)
    withTransform({
        translate(topCentre.x - 512f * k, topCentre.y - y(100f) * k)
        scale(k, k, Offset.Zero)
    }) { drawLogoGlyph(crescentColor, starColor) }
}
