package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.data.model.AnimationType
import com.mss.thebigcalendar.data.model.Language
import com.mss.thebigcalendar.ui.components.AnimationSelectionDialog
import com.mss.thebigcalendar.ui.components.LanguageSelectionDialog
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    currentTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    welcomeName: String,
    onWelcomeNameChange: (String) -> Unit,
    googleAccount: GoogleSignInAccount?,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit,
    isSyncing: Boolean = false,
    onManualSync: () -> Unit = {},
    syncProgress: com.mss.thebigcalendar.data.model.SyncProgress? = null,
    onBackClick: () -> Unit, // New parameter for back button
    onImportJsonClick: () -> Unit = {},
    currentAnimation: AnimationType = AnimationType.NONE,
    onAnimationChange: (AnimationType) -> Unit = {},
    sidebarFilterVisibility: com.mss.thebigcalendar.data.model.SidebarFilterVisibility = com.mss.thebigcalendar.data.model.SidebarFilterVisibility(),
    onToggleSidebarFilterVisibility: (String) -> Unit = {},
    currentLanguage: Language = Language.SYSTEM,
    onLanguageChange: (Language) -> Unit = {},
    onOpenCalendarVisualization: () -> Unit = {}
) {
    Log.d("GeneralSettingsScreen", "📱 GeneralSettingsScreen iniciada")
    Log.d("GeneralSettingsScreen", "🌐 Idioma atual: ${currentLanguage.displayName} (${currentLanguage.code})")
    
    val scope = rememberCoroutineScope()
    var welcomeNameInput by remember { mutableStateOf(welcomeName) }
    var showAnimationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Sincronizar o estado local com o estado do ViewModel
    LaunchedEffect(welcomeName) {
        if (welcomeNameInput != welcomeName) {
            welcomeNameInput = welcomeName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.general)) }, // Use string resource for "Geral"
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)
        ) { // Apply padding from Scaffold and add horizontal padding
            // Calendar Visualization entry
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clickable { onOpenCalendarVisualization() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.calendar_visualization_settings),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.calendar_visualization_settings_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Campo para o nome de boas-vindas
            OutlinedTextField(
                value = welcomeNameInput,
                onValueChange = {
                    welcomeNameInput = it
                    scope.launch {
                        delay(500) // Debounce para evitar muitas escritas no DataStore
                        if (welcomeNameInput == it) { // Verificar se o valor não mudou durante o delay
                            onWelcomeNameChange(it)
                        }
                    }
                },
                label = { Text(stringResource(id = R.string.welcome_name_setting_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.google_account),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                if (googleAccount != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(googleAccount.email ?: "", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onSignOutClicked) {
                            Text(stringResource(id = R.string.disconnect))
                        }
                    }
                } else {
                    Button(onClick = onSignInClicked) {
                        Text(stringResource(id = R.string.connect))
                    }
                }
            }

            // Botão de sincronização manual (só aparece quando conectado)
            if (googleAccount != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.synchronization),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onManualSync,
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.syncing))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = stringResource(id = R.string.sync_content_description),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.sync_now))
                        }
                    }

                    // Mostrar progresso detalhado se disponível
                    if (isSyncing && syncProgress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = syncProgress.currentStep,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = syncProgress.progress / 100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (syncProgress.totalEvents > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.sync_progress_format, syncProgress.processedEvents, syncProgress.totalEvents),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Botão de importar JSON
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End // Alinha para a direita
            ) {
                Button(
                    onClick = onImportJsonClick,
                    modifier = Modifier
                        .fillMaxWidth(0.65f) // Reduz para 80% da largura
                        .height(40.dp) // Altura menor
                ) {
                    Text(
                        text = stringResource(id = R.string.general_import_json_file),
                        style = MaterialTheme.typography.bodyMedium // Texto menor
                    )
                }
            }

            // Configuração de animações
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.general_animation_type),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showAnimationDialog = true },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = currentAnimation.getDisplayName(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Configuração de idioma
            Spacer(modifier = Modifier.height(16.dp))
            
            Log.d("GeneralSettingsScreen", "🎨 Renderizando seção de idioma")
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Idioma",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Idioma",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { 
                        Log.d("GeneralSettingsScreen", "🔘 Botão de idioma clicado")
                        showLanguageDialog = true 
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = "${currentLanguage.flag} ${currentLanguage.displayName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Seção "Mostrar no menu"
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(id = R.string.show_in_menu),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Lista de filtros que podem ser adicionados ao menu
            val hiddenFilters = listOf(
                "showHolidays" to stringResource(id = R.string.national_holidays),
                "showSaintDays" to stringResource(id = R.string.catholic_saint_days),
                "showEvents" to stringResource(id = R.string.events),
                "showTasks" to stringResource(id = R.string.tasks),
                "showBirthdays" to stringResource(id = R.string.birthday),
                "showNotes" to stringResource(id = R.string.note),
                "showCompletedActivities" to stringResource(id = R.string.completed_tasks_filter),
                "showMoonPhases" to stringResource(id = R.string.moon_phases_filter)
            )
            
            hiddenFilters.forEach { (key, label) ->
                val isHidden = when (key) {
                    "showHolidays" -> !sidebarFilterVisibility.showHolidays
                    "showSaintDays" -> !sidebarFilterVisibility.showSaintDays
                    "showEvents" -> !sidebarFilterVisibility.showEvents
                    "showTasks" -> !sidebarFilterVisibility.showTasks
                    "showBirthdays" -> !sidebarFilterVisibility.showBirthdays
                    "showNotes" -> !sidebarFilterVisibility.showNotes
                    "showCompletedActivities" -> !sidebarFilterVisibility.showCompletedTasks
                    "showMoonPhases" -> !sidebarFilterVisibility.showMoonPhases
                    else -> false
                }
                
                if (isHidden) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSidebarFilterVisibility(key) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Adicionar ao menu",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

        }
    }

    // Dialog de seleção de animações
    if (showAnimationDialog) {
        AnimationSelectionDialog(
            currentAnimation = currentAnimation,
            onAnimationSelected = { animationType ->
                onAnimationChange(animationType)
                showAnimationDialog = false
            },
            onDismiss = { showAnimationDialog = false }
        )
    }

    // Dialog de seleção de idioma
    if (showLanguageDialog) {
        Log.d("GeneralSettingsScreen", "📱 Exibindo diálogo de seleção de idioma")
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                Log.d("GeneralSettingsScreen", "🌐 Idioma selecionado: ${language.displayName} (${language.code})")
                onLanguageChange(language)
                showLanguageDialog = false
            },
            onDismiss = { 
                Log.d("GeneralSettingsScreen", "❌ Diálogo de idioma fechado")
                showLanguageDialog = false 
            }
        )
    }
}