package de.ricci.garminsleep.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedAuroraBackground(content: @Composable BoxScope.() -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuroraLoop")
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Orb1"
    )
    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Orb2"
    )
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF030308))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cyan = Offset(width * (0.2f + 0.6f * animOffset1), height * 0.25f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x3D00F0FF), Color.Transparent),
                    center = cyan, radius = width * 0.9f
                ), radius = width * 0.9f, center = cyan
            )
            val violet = Offset(width * (0.8f - 0.5f * animOffset2), height * 0.55f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x4DA855F7), Color.Transparent),
                    center = violet, radius = width * 0.85f
                ), radius = width * 0.85f, center = violet
            )
            val pink = Offset(width * (0.3f + 0.4f * animOffset2), height * 0.85f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x33FF0077), Color.Transparent),
                    center = pink, radius = width * 0.8f
                ), radius = width * 0.8f, center = pink
            )
        }
        content()
    }
}
