package com.mss.thebigcalendar.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Gerenciador de janelas de onboarding (primeira inicialização)
 */
class OnboardingManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OnboardingManager"
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_WELCOME_SHOWN = "welcome_shown"
        private const val KEY_GOOGLE_CONNECTED = "google_connected"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Verifica se a janela de boas-vindas já foi exibida
     */
    fun isWelcomeShown(): Boolean {
        return prefs.getBoolean(KEY_WELCOME_SHOWN, false)
    }
    
    /**
     * Marca a janela de boas-vindas como exibida
     */
    fun markWelcomeShown() {
        prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
        Log.d(TAG, "✅ Janela de boas-vindas marcada como exibida")
    }
    
    /**
     * Verifica se o Google já foi conectado
     */
    fun isGoogleConnected(): Boolean {
        return prefs.getBoolean(KEY_GOOGLE_CONNECTED, false)
    }
    
    /**
     * Marca o Google como conectado
     */
    fun markGoogleConnected() {
        prefs.edit().putBoolean(KEY_GOOGLE_CONNECTED, true).apply()
        Log.d(TAG, "✅ Google marcado como conectado")
    }
    
    /**
     * Verifica se o onboarding foi completado
     */
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    /**
     * Marca o onboarding como completado
     */
    fun markOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        Log.d(TAG, "✅ Onboarding marcado como completado")
    }
    
    /**
     * Reseta todas as configurações de onboarding (útil para testes)
     */
    fun resetOnboarding() {
        prefs.edit().clear().apply()
        Log.d(TAG, "🔄 Onboarding resetado")
    }
    
    /**
     * Verifica se deve exibir alguma janela de onboarding
     */
    fun shouldShowOnboarding(): Boolean {
        return !isOnboardingCompleted()
    }
    
    /**
     * Verifica se deve exibir a janela de boas-vindas
     */
    fun shouldShowWelcome(): Boolean {
        return !isWelcomeShown()
    }
}

/**
 * Composable para a janela de boas-vindas
 */
@Composable
fun WelcomeDialog(
    onDismiss: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone de boas-vindas
                Text(
                    text = "🎉",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Título
                Text(
                    text = "Bem-vindo ao TheBigCalendar!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Subtítulo
                Text(
                    text = "Seu calendário pessoal e organizado",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Descrição
                Text(
                    text = "Para uma experiência completa, conecte sua conta do Google e sincronize seus eventos.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Botão conectar Google
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Conectar Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Botão pular
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pular por enquanto",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Composable para gerenciar o fluxo de onboarding
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    val context = LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }
    
    var showWelcome by remember { mutableStateOf(false) }
    
    // Verificar se deve exibir onboarding
    LaunchedEffect(Unit) {
        if (onboardingManager.shouldShowOnboarding() && onboardingManager.shouldShowWelcome()) {
            showWelcome = true
        } else {
            onComplete()
        }
    }
    
    // Janela de boas-vindas
    if (showWelcome) {
        WelcomeDialog(
            onDismiss = {
                showWelcome = false
                onboardingManager.markWelcomeShown()
                onComplete()
            },
            onGoogleSignIn = {
                // Chama a função de login do Google existente
                onGoogleSignIn()
                showWelcome = false
                onboardingManager.markWelcomeShown()
                onComplete()
            },
            onSkip = {
                showWelcome = false
                onboardingManager.markWelcomeShown()
                onComplete()
            }
        )
    }
}
