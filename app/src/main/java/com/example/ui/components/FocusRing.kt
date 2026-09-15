package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FocusRing(
    focusPoint: Offset?,
    onFocusAnimationFinished: () -> Unit
) {
    if (focusPoint == null) return

    val scaleAnim = remember { Animatable(1.5f) }
    val alphaAnim = remember { Animatable(1.0f) }

    LaunchedEffect(focusPoint) {
        scaleAnim.snapTo(1.5f)
        alphaAnim.snapTo(1.0f)

        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 250)
        )
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400, delayMillis = 400)
        )
        onFocusAnimationFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = 32.dp.toPx() * scaleAnim.value
        val strokeWidth = 2.dp.toPx()
        val color = Color(0xFF00E5FF).copy(alpha = alphaAnim.value)

        // Focus Circle
        drawCircle(
            color = color,
            radius = radius,
            center = focusPoint,
            style = Stroke(width = strokeWidth)
        )

        // Crosshairs
        val tick = 8.dp.toPx()
        drawLine(
            color = color,
            start = Offset(focusPoint.x - radius - tick, focusPoint.y),
            end = Offset(focusPoint.x - radius, focusPoint.y),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(focusPoint.x + radius, focusPoint.y),
            end = Offset(focusPoint.x + radius + tick, focusPoint.y),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(focusPoint.x, focusPoint.y - radius - tick),
            end = Offset(focusPoint.x, focusPoint.y - radius),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(focusPoint.x, focusPoint.y + radius),
            end = Offset(focusPoint.x, focusPoint.y + radius + tick),
            strokeWidth = strokeWidth
        )
    }
}
