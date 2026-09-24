package com.airobot.framework.util

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Skeuomorphic Inner Shadow Extension for Jetpack Compose.
 */
fun Modifier.insetShadow(
    color: Color,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    blurRadius: Dp = 4.dp,
    spreadRadius: Dp = 0.dp,
    shape: Shape
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    
    val paint = Paint().apply {
        this.color = color
    }
    
    val frameworkPaint = paint.asFrameworkPaint().apply {
        if (blurRadius.toPx() > 0) {
            maskFilter = BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    }
    
    onDrawWithContent {
        drawContent()
        
        val shadowOffsetX = offsetX.toPx()
        val shadowOffsetY = offsetY.toPx()
        val shadowSpread = spreadRadius.toPx()
        
        // Save the canvas state
        drawIntoCanvas { canvas ->
            canvas.save()
            // Clip to the shape bounds
            canvas.clipPath(path)
            
            // Create a larger path that encompasses the outer area
            val bounds = outline.bounds
            val shadowPath = Path().apply {
                addRect(
                    Rect(
                        left = bounds.left - shadowSpread - blurRadius.toPx(),
                        top = bounds.top - shadowSpread - blurRadius.toPx(),
                        right = bounds.right + shadowSpread + blurRadius.toPx(),
                        bottom = bounds.bottom + shadowSpread + blurRadius.toPx()
                    )
                )
                
                // Cut out the actual shape, offset by shadow offset
                val innerPath = Path().apply {
                    addOutline(shape.createOutline(
                        Size(size.width - shadowSpread * 2, size.height - shadowSpread * 2),
                        layoutDirection,
                        this@drawWithCache
                    ))
                }
                
                // Shift inner path
                innerPath.translate(Offset(shadowOffsetX + shadowSpread, shadowOffsetY + shadowSpread))
                
                // Set fill type to EvenOdd to create a "hole"
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                addPath(innerPath)
            }
            
            // Draw the shadow
            canvas.drawPath(shadowPath, paint)
            
            canvas.restore()
        }
    }
}
