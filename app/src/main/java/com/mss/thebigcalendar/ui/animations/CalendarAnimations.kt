package com.mss.thebigcalendar.ui.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mss.thebigcalendar.data.model.AnimationType

@Composable
fun CalendarAnimationState(
    isAnimating: Boolean,
    animationDirection: Float,
    animationType: AnimationType
): AnimationValues {
    return when (animationType) {
        AnimationType.SLIDE -> slideAnimation(isAnimating, animationDirection)
        AnimationType.REVEAL -> revealAnimation(isAnimating, animationDirection)
        AnimationType.FADE -> fadeAnimation(isAnimating, animationDirection)
        AnimationType.SCALE -> scaleAnimation(isAnimating, animationDirection)
        AnimationType.ROTATION -> rotationAnimation(isAnimating, animationDirection)
        AnimationType.BOUNCE -> bounceAnimation(isAnimating, animationDirection)
        AnimationType.NONE -> noAnimation()
    }
}

@Composable
private fun slideAnimation(isAnimating: Boolean, animationDirection: Float): AnimationValues {
    val translationX by animateFloatAsState(
        targetValue = if (isAnimating) -animationDirection * 300f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "slideTranslationX"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.0f else 1f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "slideAlpha"
    )
    
    return AnimationValues(
        translationX = translationX,
        translationY = 0f,
        scaleX = 1f,
        scaleY = 1f,
        rotationX = 0f,
        rotationY = 0f,
        rotationZ = 0f,
        alpha = alpha,
        shadowElevation = if (isAnimating) 6f else 2f
    )
}

@Composable
private fun revealAnimation(isAnimating: Boolean, animationDirection: Float): AnimationValues {
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 0.8f else 1f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "revealScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.0f else 1f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "revealAlpha"
    )
    
    val rotationZ by animateFloatAsState(
        targetValue = if (isAnimating) 5f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "revealRotationZ"
    )
    
    return AnimationValues(
        translationX = 0f,
        translationY = 0f,
        scaleX = scale,
        scaleY = scale,
        rotationX = 0f,
        rotationY = 0f,
        rotationZ = rotationZ,
        alpha = alpha,
        shadowElevation = if (isAnimating) 12f else 2f
    )
}

@Composable
private fun fadeAnimation(isAnimating: Boolean, animationDirection: Float): AnimationValues {
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.0f else 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "fadeAlpha"
    )
    
    return AnimationValues(
        translationX = 0f,
        translationY = 0f,
        scaleX = 1f,
        scaleY = 1f,
        rotationX = 0f,
        rotationY = 0f,
        rotationZ = 0f,
        alpha = alpha,
        shadowElevation = if (isAnimating) 4f else 2f
    )
}

@Composable
private fun scaleAnimation(isAnimating: Boolean, animationDirection: Float): AnimationValues {
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 0.5f else 1f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "scaleAnimation"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.0f else 1f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "scaleAlpha"
    )
    
    return AnimationValues(
        translationX = 0f,
        translationY = 0f,
        scaleX = scale,
        scaleY = scale,
        rotationX = 0f,
        rotationY = 0f,
        rotationZ = 0f,
        alpha = alpha,
        shadowElevation = if (isAnimating) 8f else 2f
    )
}

@Composable
private fun rotationAnimation(isAnimating: Boolean, animationDirection: Float): AnimationValues {
    val rotationY by animateFloatAsState(
        targetValue = if (isAnimating) animationDirection * 90f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "rotationY"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 0.8f else 1f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "rotationScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.0f else 1f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "rotationAlpha"
    )
    
    return AnimationValues(
        translationX = 0f,
        translationY = 0f,
        scaleX = scale,
        scaleY = scale,
        rotationX = 0f,
        rotationY = rotationY,
        rotationZ = 0f,
        alpha = alpha,
        shadowElevation = if (isAnimating) 10f else 2f
    )
}

@Composable
private fun bounceAnimation(isAnimating: Boolean, animationDirection: Float): AnimationValues {
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 0.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0.0f else 1f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "bounceAlpha"
    )
    
    return AnimationValues(
        translationX = 0f,
        translationY = 0f,
        scaleX = scale,
        scaleY = scale,
        rotationX = 0f,
        rotationY = 0f,
        rotationZ = 0f,
        alpha = alpha,
        shadowElevation = if (isAnimating) 15f else 2f
    )
}

@Composable
private fun noAnimation(): AnimationValues {
    return AnimationValues(
        translationX = 0f,
        translationY = 0f,
        scaleX = 1f,
        scaleY = 1f,
        rotationX = 0f,
        rotationY = 0f,
        rotationZ = 0f,
        alpha = 1f,
        shadowElevation = 2f
    )
}

data class AnimationValues(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val alpha: Float,
    val shadowElevation: Float
)



















