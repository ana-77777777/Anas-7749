package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.threeDTiltEffect(
    maxRotationDegrees: Float = 8f
): Modifier = composed {
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    val animatedRotationX by animateFloatAsState(
        targetValue = if (isPressed) rotationX else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = if (isPressed) rotationY else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "rotationY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "scale"
    )

    this
        .graphicsLayer {
            this.rotationX = animatedRotationX
            this.rotationY = animatedRotationY
            this.scaleX = animatedScale
            this.scaleY = animatedScale
            this.cameraDistance = 16f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    isPressed = true
                    val width = size.width
                    val height = size.height
                    if (width > 0 && height > 0) {
                        val xFactor = ((offset.x / width) - 0.5f) * 2f
                        val yFactor = ((offset.y / height) - 0.5f) * 2f
                        rotationY = xFactor * maxRotationDegrees
                        rotationX = -yFactor * maxRotationDegrees
                    }
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

fun Modifier.threeDShadow(
    elevation: Dp = 8.dp
): Modifier = composed {
    this.graphicsLayer {
        this.shadowElevation = elevation.toPx()
        this.shape = RoundedCornerShape(20.dp)
        this.clip = false
    }
}
