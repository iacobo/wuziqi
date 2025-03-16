package com.iacobo.wuziqi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

/**
 * Helper to animate a value with a properly scoped coroutine.
 */
@Composable
fun AnimateValue(
    animatable: Animatable<Float, AnimationVector1D>,
    targetValue: Float,
    durationMillis: Int,
    onAnimationFinished: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = durationMillis
            )
        )
        onAnimationFinished()
    }
}

/**
 * Extension function to help with launching coroutines safely from @Composable functions.
 */
@Composable
fun rememberAnimationCoroutineScope(): CoroutineScope {
    return rememberCoroutineScope()
}