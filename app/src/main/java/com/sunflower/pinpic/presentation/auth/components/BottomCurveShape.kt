package com.sunflower.pinpic.presentation.auth.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class BottomCurveShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            // Drop down further on the right edge
            lineTo(size.width, size.height * 1.0f)

            // Inverted arch (Upside-down U):
            // The control point pulls the center UP (e.g., to 70% height),
            // while the end point matches the deeper drop on the left edge.
            quadraticTo(
                size.width * 0.5f, size.height * 0.70f, // Control point apex (curves upward)
                0f, size.height * 1.0f                 // End point deep left
            )
            close()
        }
        return Outline.Generic(path)
    }
}