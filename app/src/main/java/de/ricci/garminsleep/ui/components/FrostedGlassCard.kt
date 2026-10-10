package de.ricci.garminsleep.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    glowColors: List<Color> = listOf(
        Color(0xFF00F0FF), // Neon Cyan
        Color(0xFFA855F7), // Neon Purple
        Color(0xFFFF007F)  // Neon Pink
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 550f),
        label = "glassPressScale"
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "glassPressGlow"
    )
    val shape = RoundedCornerShape(cornerRadius)
    val glowBrush = Brush.linearGradient(glowColors)

    // Subtiler Lichteinfall auf dem Milchglas
    val glassFill = Brush.verticalGradient(
        listOf(
            Color(0x24FFFFFF), // Sanfter Glanz oben
            Color(0x0AFFFFFF)  // Dunkler unten
        )
    )

    Box(modifier = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .then(if (onClick != null) Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ) else Modifier)
    ) {
        // --- 1. SCHICHT: WEITER DIFFUSER HALO (AUSSEN) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(35f, 35f, Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                        alpha = 0.55f + pressGlow * 0.25f // Intensität des Außenlichts
                    }
                    .border(width = 4.dp, brush = glowBrush, shape = shape)
            )

            // --- 2. SCHICHT: INTENSIVER KERN-GLOW ---
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = RenderEffect
                            .createBlurEffect(12f, 12f, Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                        alpha = 0.85f + pressGlow * 0.15f // Kräftiges Leuchten direkt am Rand
                    }
                    .border(width = 2.5.dp, brush = glowBrush, shape = shape)
            )
        }

        // --- 3. SCHICHT: EIGENTLICHE GLASKARTE MIT FEINER KANTE ---
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassFill)
                .border(
                    width = 1.2.dp,
                    brush = glowBrush,
                    shape = shape
                )
        )

        // Inhalt der Karte mit Innenabstand
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            content()
        }
    }
}
