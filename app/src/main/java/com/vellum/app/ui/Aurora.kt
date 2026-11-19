package com.vellum.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

/** Slow drifting colour blobs. Violet/rose when off, teal/mint when connected. */
@Composable
fun AuroraBackground(connected: Boolean, animated: Boolean = true, modifier: Modifier = Modifier) {
    val a by animateColorAsState(if (connected) Color(0xFF14B8A6) else Color(0xFF7C5CFF), tween(1400), label = "a")
    val b by animateColorAsState(if (connected) Color(0xFF4ADE80) else Color(0xFFFF6B9A), tween(1400), label = "b")
    val c by animateColorAsState(if (connected) Color(0xFF2B9BFF) else Color(0xFF2B6BFF), tween(1400), label = "c")

    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )

    Canvas(modifier.fillMaxSize().background(Palette.Base)) {
        fun wave(phase: Float) = 0.5f + 0.5f * sin(2f * PI.toFloat() * (t + phase))
        val w = size.width
        val h = size.height
        val r = size.maxDimension * 0.55f

        fun blob(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy), radius = radius
                ),
                radius = radius, center = Offset(cx, cy)
            )
        }
        blob(a, w * (0.15f + 0.55f * wave(0.00f)), h * (0.12f + 0.25f * wave(0.30f)), r, 0.55f)
        blob(b, w * (0.85f - 0.55f * wave(0.20f)), h * (0.55f + 0.30f * wave(0.60f)), r * 0.9f, 0.40f)
        blob(c, w * (0.20f + 0.60f * wave(0.55f)), h * (0.90f - 0.25f * wave(0.10f)), r * 0.8f, 0.45f)
    }
}
