package com.mss.thebigcalendar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlin.math.max
import kotlin.math.min

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Gera um esquema de cores completo baseado na cor primária personalizada
 * Seguindo as diretrizes do Material Design 3 para cores harmoniosas
 */
private fun generateCustomColorScheme(
    primaryColor: Color,
    darkTheme: Boolean,
    pureBlack: Boolean
): androidx.compose.material3.ColorScheme {
    val hsl = rgbToHsl(primaryColor)
    
    // Gerar cores secundárias e terciárias baseadas na cor primária
    val secondaryHsl = hsl.copy(h = (hsl.h + 60f) % 360f) // 60 graus de diferença
    val tertiaryHsl = hsl.copy(h = (hsl.h + 120f) % 360f) // 120 graus de diferença
    
    val secondaryColor = hslToRgb(secondaryHsl)
    val tertiaryColor = hslToRgb(tertiaryHsl)
    
    if (darkTheme) {
        return darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.3f),
            onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
            
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = secondaryColor.copy(alpha = 0.2f),
            onSecondaryContainer = secondaryColor.copy(alpha = 0.9f),
            
            tertiary = tertiaryColor,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryColor.copy(alpha = 0.2f),
            onTertiaryContainer = tertiaryColor.copy(alpha = 0.9f),
            
            background = if (pureBlack) Color.Black else Color(0xFF121212),
            onBackground = Color(0xFFE6E1E5),
            surface = if (pureBlack) Color.Black else Color(0xFF121212),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            
            outline = Color(0xFF938F99),
            outlineVariant = Color(0xFF49454F),
            
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        return lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
            
            secondary = secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = secondaryColor.copy(alpha = 0.1f),
            onSecondaryContainer = secondaryColor.copy(alpha = 0.9f),
            
            tertiary = tertiaryColor,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryColor.copy(alpha = 0.1f),
            onTertiaryContainer = tertiaryColor.copy(alpha = 0.9f),
            
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            
            outline = Color(0xFF79747E),
            outlineVariant = Color(0xFFCAC4D0),
            
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}

/**
 * Converte RGB para HSL para manipulação de cores
 */
private data class HslColor(val h: Float, val s: Float, val l: Float)

private fun rgbToHsl(color: Color): HslColor {
    val r = color.red
    val g = color.green
    val b = color.blue
    
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    
    val l = (max + min) / 2f
    
    val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))
    
    val h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta) % 6f * 60f
        max == g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }
    
    return HslColor(
        h = if (h < 0) h + 360f else h,
        s = s,
        l = l
    )
}

/**
 * Converte HSL para RGB
 */
private fun hslToRgb(hsl: HslColor): Color {
    val h = hsl.h / 360f
    val s = hsl.s
    val l = hsl.l
    
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((h * 6f) % 2f - 1f))
    val m = l - c / 2f
    
    val (r, g, b) = when {
        h < 1f/6f -> Triple(c, x, 0f)
        h < 2f/6f -> Triple(x, c, 0f)
        h < 3f/6f -> Triple(0f, c, x)
        h < 4f/6f -> Triple(0f, x, c)
        h < 5f/6f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f)
    )
}

@Composable
fun TheBigCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    primaryColorHex: String = "AUTO",
    content: @Composable () -> Unit
) {
    // Gerar esquema de cores baseado na configuração
    val colorScheme = if (primaryColorHex == "AUTO") {
        // Modo automático: usar cores dinâmicas do Android
        val context = LocalContext.current
        val baseScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        
        if (pureBlack && darkTheme) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black
            )
        } else {
            baseScheme
        }
    } else {
        // Modo manual: usar cor personalizada
        val customPrimaryColor = Color(android.graphics.Color.parseColor(primaryColorHex))
        generateCustomColorScheme(customPrimaryColor, darkTheme, pureBlack)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}