package com.lenohutapea.plantidentifier.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A small line-icon set drawn for this app (24dp grid, 1.8 stroke). Tint them with Icon(tint = ...). */
object PlantIcons {
    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, false, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, false, -2 * r, 0f)
    }

    val Camera = icon("Camera") {
        moveTo(4f, 8f); lineTo(7.5f, 8f); lineTo(9f, 5.5f); lineTo(15f, 5.5f); lineTo(16.5f, 8f); lineTo(20f, 8f)
        lineTo(20f, 19f); lineTo(4f, 19f); close()
        circle(12f, 13.2f, 3.4f)
    }

    val Gallery = icon("Gallery") {
        moveTo(3.5f, 5f); lineTo(20.5f, 5f); lineTo(20.5f, 19f); lineTo(3.5f, 19f); close()
        moveTo(3.5f, 16f); lineTo(8.5f, 11f); lineTo(12.5f, 15f); lineTo(15f, 12.5f); lineTo(20.5f, 17.5f)
        circle(15.5f, 8.8f, 1.4f)
    }

    val Leaf = icon("Leaf") {
        moveTo(5f, 19f); curveTo(5f, 10.5f, 10.5f, 5f, 19f, 5f); curveTo(19f, 13.5f, 13.5f, 19f, 5f, 19f); close()
        moveTo(5f, 19f); lineTo(13.5f, 10.5f)
    }

    val Water = icon("Water") {
        moveTo(12f, 3.5f); curveTo(12f, 3.5f, 6f, 10f, 6f, 14f)
        curveTo(6f, 17.3f, 8.7f, 20f, 12f, 20f); curveTo(15.3f, 20f, 18f, 17.3f, 18f, 14f)
        curveTo(18f, 10f, 12f, 3.5f, 12f, 3.5f); close()
    }

    val Sprout = icon("Sprout") {
        moveTo(12f, 20f); lineTo(12f, 11f)
        moveTo(12f, 13.5f); curveTo(12f, 9.5f, 9f, 7.5f, 5f, 7.5f); curveTo(5f, 11.5f, 8f, 13.5f, 12f, 13.5f)
        moveTo(12f, 11f); curveTo(12f, 7f, 15f, 4.5f, 19f, 4.5f); curveTo(19f, 8.5f, 16f, 11f, 12f, 11f)
        moveTo(7f, 20f); lineTo(17f, 20f)
    }

    val Fertilizer = icon("Fertilizer") {
        moveTo(9f, 4f); lineTo(15f, 4f); lineTo(13.6f, 7f)
        curveTo(17.4f, 8.8f, 19f, 12.6f, 19f, 15.8f); curveTo(19f, 19f, 16.8f, 20f, 12f, 20f)
        curveTo(7.2f, 20f, 5f, 19f, 5f, 15.8f); curveTo(5f, 12.6f, 6.6f, 8.8f, 10.4f, 7f); close()
        moveTo(10.4f, 7f); lineTo(13.6f, 7f)
    }

    val Harvest = icon("Harvest") {
        moveTo(12f, 21f); lineTo(12f, 5f)
        moveTo(12f, 9f); curveTo(9.2f, 9f, 8f, 7.2f, 8f, 5f); curveTo(10.2f, 5f, 12f, 6.6f, 12f, 9f)
        moveTo(12f, 9f); curveTo(14.8f, 9f, 16f, 7.2f, 16f, 5f); curveTo(13.8f, 5f, 12f, 6.6f, 12f, 9f)
        moveTo(12f, 13.5f); curveTo(9.2f, 13.5f, 8f, 11.7f, 8f, 9.5f); curveTo(10.2f, 9.5f, 12f, 11.1f, 12f, 13.5f)
        moveTo(12f, 13.5f); curveTo(14.8f, 13.5f, 16f, 11.7f, 16f, 9.5f); curveTo(13.8f, 9.5f, 12f, 11.1f, 12f, 13.5f)
    }

    val Place = icon("Place") {
        moveTo(12f, 21f); curveTo(12f, 21f, 5.5f, 14.8f, 5.5f, 10f); curveTo(5.5f, 6.4f, 8.4f, 3.5f, 12f, 3.5f)
        curveTo(15.6f, 3.5f, 18.5f, 6.4f, 18.5f, 10f); curveTo(18.5f, 14.8f, 12f, 21f, 12f, 21f); close()
        circle(12f, 10f, 2.3f)
    }

    val Shield = icon("Shield") {
        moveTo(12f, 3.5f); lineTo(19f, 6f); lineTo(19f, 11.5f); curveTo(19f, 16f, 16f, 19.3f, 12f, 20.5f)
        curveTo(8f, 19.3f, 5f, 16f, 5f, 11.5f); lineTo(5f, 6f); close()
        moveTo(9f, 12f); lineTo(11.2f, 14.2f); lineTo(15.2f, 10f)
    }

    val Lesion = icon("Lesion") {
        moveTo(5f, 19f); curveTo(5f, 10.5f, 10.5f, 5f, 19f, 5f); curveTo(19f, 13.5f, 13.5f, 19f, 5f, 19f); close()
        circle(11f, 12.5f, 1.3f)
        circle(14.3f, 9.3f, 0.9f)
        circle(9.2f, 15.6f, 0.8f)
    }
}
