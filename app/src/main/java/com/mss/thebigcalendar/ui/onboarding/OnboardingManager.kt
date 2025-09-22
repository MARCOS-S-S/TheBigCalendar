package com.mss.thebigcalendar.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mss.thebigcalendar.R

/**
 * Gerenciador de janelas de onboarding (primeira inicialização)
 */
class OnboardingManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OnboardingManager"
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_WELCOME_SHOWN = "welcome_shown"

        private const val KEY_STORAGE_PERMISSION_SHOWN = "storage_permission_shown"
        private const val KEY_GOOGLE_CONNECTED = "google_connected"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_NOTIFICATION_PERMISSION_SHOWN = "notification_permission_shown"
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
    }
    

    
    /**
     * Verifica se a janela de permissão de armazenamento já foi exibida
     */
    fun isStoragePermissionShown(): Boolean {
        return prefs.getBoolean(KEY_STORAGE_PERMISSION_SHOWN, false)
    }
    
    /**
     * Marca a janela de permissão de armazenamento como exibida
     */
    fun markStoragePermissionShown() {
        prefs.edit().putBoolean(KEY_STORAGE_PERMISSION_SHOWN, true).apply()
    }

    /**
     * Verifica se a janela de permissão de notificação já foi exibida
     */
    fun isNotificationPermissionShown(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_SHOWN, false)
    }

    /**
     * Marca a janela de permissão de notificação como exibida
     */
    fun markNotificationPermissionShown() {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_SHOWN, true).apply()
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
    }
    
    /**
     * Reseta todas as configurações de onboarding (útil para testes)
     */
    fun resetOnboarding() {
        prefs.edit().clear().apply()
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
    
    /**
     * Verifica se deve exibir a janela de permissão de armazenamento
     */
    fun shouldShowStoragePermission(): Boolean {
        return isWelcomeShown() && !isStoragePermissionShown()
    }

    /**
     * Verifica se deve exibir a janela de permissão de notificação
     */
    fun shouldShowNotificationPermission(): Boolean {
        return isStoragePermissionShown() && !isNotificationPermissionShown()
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
 * Composable para a janela de permissão de armazenamento
 */
@Composable
fun StoragePermissionDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
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
                // Ícone de armazenamento
                Text(
                    text = "💾",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Título
                Text(
                    text = "Permissão de Armazenamento",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Subtítulo
                Text(
                    text = "Para salvar e restaurar seus backups",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Descrição
                Text(
                    text = "O TheBigCalendar precisa de acesso ao armazenamento para criar backups dos seus agendamentos e permitir que você os restaure quando necessário.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Botão solicitar permissão
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Conceder Permissão",
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
 * Composable para a janela de permissão de notificação
 */
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
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
                // Ícone de notificação
                Text(
                    text = "🔔",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Título
                Text(
                    text = "Permissão de Notificação",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Subtítulo
                Text(
                    text = "Para não perder seus compromissos",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Descrição
                Text(
                    text = "O TheBigCalendar usa notificações para te lembrar de eventos e tarefas importantes. Permita o envio de notificações para aproveitar ao máximo o aplicativo.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Botão solicitar permissão
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Permitir Notificações",
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
    onGoogleSignIn: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }

    var showWelcome by remember { mutableStateOf(false) }
    var showStoragePermission by remember { mutableStateOf(false) }
    var showNotificationPermission by remember { mutableStateOf(false) }
    
    // Verificar se deve exibir onboarding
    LaunchedEffect(Unit) {
        when {
            onboardingManager.shouldShowWelcome() -> showWelcome = true
            onboardingManager.shouldShowStoragePermission() -> showStoragePermission = true
            onboardingManager.shouldShowNotificationPermission() -> showNotificationPermission = true
            else -> onComplete()
        }
    }

    // Tela de fundo com imagem - apenas quando há onboarding ativo
    if (showWelcome || showStoragePermission || showNotificationPermission) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagem de fundo
            Image(
                painter = painterResource(id = R.drawable.tbc_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Overlay escuro para melhorar legibilidade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
            // Janela de boas-vindas
            if (showWelcome) {
            WelcomeDialog(
                onDismiss = {
                    showWelcome = false
                    onboardingManager.markWelcomeShown()
                    if (onboardingManager.shouldShowStoragePermission()) {
                        showStoragePermission = true
                    } else if (onboardingManager.shouldShowNotificationPermission()) {
                        showNotificationPermission = true
                    } else {
                        onComplete()
                    }
                },
                onGoogleSignIn = {
                    onGoogleSignIn()
                    showWelcome = false
                    onboardingManager.markWelcomeShown()
                    if (onboardingManager.shouldShowStoragePermission()) {
                        showStoragePermission = true
                    } else if (onboardingManager.shouldShowNotificationPermission()) {
                        showNotificationPermission = true
                    } else {
                        onComplete()
                    }
                },
                onSkip = {
                    showWelcome = false
                    onboardingManager.markWelcomeShown()
                    if (onboardingManager.shouldShowStoragePermission()) {
                        showStoragePermission = true
                    } else if (onboardingManager.shouldShowNotificationPermission()) {
                        showNotificationPermission = true
                    } else {
                        onComplete()
                    }
                }
            )
            }
            
            // Janela de permissão de armazenamento
            if (showStoragePermission) {
            StoragePermissionDialog(
                onDismiss = {
                    showStoragePermission = false
                    onboardingManager.markStoragePermissionShown()
                    if (onboardingManager.shouldShowNotificationPermission()) {
                        showNotificationPermission = true
                    } else {
                        onComplete()
                    }
                },
                onRequestPermission = {
                    onRequestStoragePermission()
                    showStoragePermission = false
                    onboardingManager.markStoragePermissionShown()
                    if (onboardingManager.shouldShowNotificationPermission()) {
                        showNotificationPermission = true
                    } else {
                        onComplete()
                    }
                },
                onSkip = {
                    showStoragePermission = false
                    onboardingManager.markStoragePermissionShown()
                    if (onboardingManager.shouldShowNotificationPermission()) {
                        showNotificationPermission = true
                    } else {
                        onComplete()
                    }
                }
                )
            }

            // Janela de permissão de notificação
            if (showNotificationPermission) {
            NotificationPermissionDialog(
                onDismiss = {
                    showNotificationPermission = false
                    onboardingManager.markNotificationPermissionShown()
                    onComplete()
                },
                onRequestPermission = {
                    onRequestNotificationPermission()
                    showNotificationPermission = false
                    onboardingManager.markNotificationPermissionShown()
                    onComplete()
                },
                onSkip = {
                    showNotificationPermission = false
                    onboardingManager.markNotificationPermissionShown()
                    onComplete()
                }
                )
            }
        }
    }
}
