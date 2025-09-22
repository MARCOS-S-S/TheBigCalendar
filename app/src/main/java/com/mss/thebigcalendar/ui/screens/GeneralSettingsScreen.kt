package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.background
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
import com.mss.thebigcalendar.ui.components.AnimationSelectionDialog
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
    onAnimationChange: (AnimationType) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var welcomeNameInput by remember { mutableStateOf(welcomeName) }
    var showAnimationDialog by remember { mutableStateOf(false) }

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)
        ) { // Apply padding from Scaffold and add horizontal padding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.sidebar_change_theme),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = when (currentTheme) {
                        Theme.LIGHT -> false
                        Theme.DARK -> true
                        Theme.SYSTEM -> isSystemInDarkTheme()
                    },
                    onCheckedChange = { isChecked ->
                        onThemeChange(if (isChecked) Theme.DARK else Theme.LIGHT)
                    }
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
                        text = "Importar Arquivo JSON",
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
                    text = "Tipo de Animação",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showAnimationDialog = true },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = currentAnimation.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
}