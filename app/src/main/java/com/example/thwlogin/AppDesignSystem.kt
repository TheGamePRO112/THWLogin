package com.example.thwlogin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tactile spring press feedback inspired by Apple & Emil Kowalski's Design Engineering philosophy.
 * Instant response on pointer-down with fluid spring recovery.
 */
fun Modifier.tactilePress(
    enabled: Boolean = true,
    pressScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tactileScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    }
                )
            }
        }
}

/**
 * iOS-Style Grouped Glass Card Container.
 */
@Composable
fun IosGroupedCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    cornerRadius: Dp = 26.dp,
    content: @Composable () -> Unit
) {
    val bgBrush = if (isDarkMode) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color(0xFF0F172A).copy(alpha = 0.72f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.94f),
                Color(0xFFE2ECF8).copy(alpha = 0.82f)
            )
        )
    }

    val borderBrush = if (isDarkMode) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White,
                Color.White.copy(alpha = 0.55f)
            )
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, borderBrush)
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush, RoundedCornerShape(cornerRadius))
        ) {
            content()
        }
    }
}

/**
 * Backward compatibility alias for AppleGlassCard
 */
@Composable
fun AppleGlassCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    cornerRadius: Dp = 26.dp,
    content: @Composable () -> Unit
) {
    IosGroupedCard(
        modifier = modifier,
        isDarkMode = isDarkMode,
        cornerRadius = cornerRadius,
        content = content
    )
}

/**
 * Authentic iOS Segmented Control with animated selection pill.
 */
@Composable
fun IosSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    val containerBg = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val activePillColor = if (isDarkMode) Color(0xFF0055FF) else Color(0xFF003399)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerBg,
        border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(3.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    animationSpec = tween(200),
                    label = "textColor"
                )

                Surface(
                    onClick = { onOptionSelected(index) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) activePillColor else Color.Transparent,
                    shadowElevation = if (isSelected) 4.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .tactilePress { onOptionSelected(index) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS Tactile Action Button.
 */
@Composable
fun IosButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val defaultContainer = containerColor ?: if (isDarkMode) {
        Color(0xFF0055FF)
    } else {
        Color(0xFF003399)
    }

    val defaultContent = contentColor ?: Color.White

    val borderColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.35f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.tactilePress(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = defaultContainer,
            contentColor = defaultContent
        ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
        content = content
    )
}

/**
 * Backward compatibility alias for AppleGlassButton
 */
@Composable
fun AppleGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    IosButton(
        onClick = onClick,
        modifier = modifier,
        isDarkMode = isDarkMode,
        containerColor = containerColor,
        contentColor = contentColor,
        enabled = enabled,
        content = content
    )
}

/**
 * iOS Badge Chip for Status & Version tags.
 */
@Composable
fun IosChip(
    text: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFFFF9800),
    textColor: Color = Color(0xFFFFB74D)
) {
    Surface(
        modifier = modifier,
        color = badgeColor.copy(alpha = 0.22f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.8f))
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * Backward compatibility alias for AppleGlassChip
 */
@Composable
fun AppleGlassChip(
    text: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFFFF9800),
    textColor: Color = Color(0xFFFFB74D)
) {
    IosChip(
        text = text,
        modifier = modifier,
        badgeColor = badgeColor,
        textColor = textColor
    )
}
