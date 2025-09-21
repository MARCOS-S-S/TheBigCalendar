package com.mss.thebigcalendar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Drawer customizado com animação de abertura controlada
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDrawer(
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    scrimColor: Color = Color.Black.copy(alpha = 0.7f),
    animationDuration: Int = 600, // Duração da animação de abertura
    drawerContent: @Composable ColumnScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Animação de abertura customizada
    val drawerWidth = 320.dp
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    
    val drawerOffset by animateFloatAsState(
        targetValue = when {
            drawerState.isOpen -> 0f
            drawerState.targetValue == DrawerValue.Open -> 0f
            else -> -drawerWidthPx
        },
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = EaseInOutCubic
        ),
        label = "drawerOffset"
    )
    
    // Animação do scrim (fundo escurecido)
    val scrimAlpha by animateFloatAsState(
        targetValue = if (drawerState.isOpen || drawerState.targetValue == DrawerValue.Open) 0.7f else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = EaseInOutCubic
        ),
        label = "scrimAlpha"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // Conteúdo principal
        content()
        
        // Scrim (fundo escurecido) com clique para fechar
        if (scrimAlpha > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            drawerState.close()
                        }
                    }
            )
        }
        
        // Drawer com animação de abertura customizada
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .graphicsLayer {
                    translationX = drawerOffset
                }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
            content = drawerContent
        )
    }
}
