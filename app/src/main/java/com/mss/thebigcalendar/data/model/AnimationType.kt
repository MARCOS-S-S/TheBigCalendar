package com.mss.thebigcalendar.data.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mss.thebigcalendar.R

enum class AnimationType {
    SLIDE,
    REVEAL,
    FADE,
    SCALE,
    ROTATION,
    BOUNCE,
    NONE;
    
    @Composable
    fun getDisplayName(): String {
        return when (this) {
            SLIDE -> stringResource(id = R.string.animation_slide)
            REVEAL -> stringResource(id = R.string.animation_reveal)
            FADE -> stringResource(id = R.string.animation_fade)
            SCALE -> stringResource(id = R.string.animation_scale)
            ROTATION -> stringResource(id = R.string.animation_rotation)
            BOUNCE -> stringResource(id = R.string.animation_bounce)
            NONE -> stringResource(id = R.string.animation_none)
        }
    }
}














