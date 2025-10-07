package com.mss.thebigcalendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.screens.SearchScreen
import com.mss.thebigcalendar.ui.screens.TrashScreen
import com.mss.thebigcalendar.ui.screens.ChartScreen
import com.mss.thebigcalendar.ui.screens.SchedulesScreen
import com.mss.thebigcalendar.ui.screens.AlarmsScreen
import com.mss.thebigcalendar.ui.screens.GeneralSettingsScreen
import com.mss.thebigcalendar.ui.screens.CompletedTasksScreen
import com.mss.thebigcalendar.ui.screens.PrintCalendarScreen
import com.mss.thebigcalendar.ui.screens.BackupScreen
import com.mss.thebigcalendar.ui.screens.JsonConfigScreen
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import com.mss.thebigcalendar.ui.onboarding.OnboardingFlow
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import com.mss.thebigcalendar.ui.screens.CalendarVisualizationSettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CalendarViewModel
    
    // ✅ Flag para detectar se o app já estava em execução
    companion object {
        private var isAppAlreadyRunning = false
        private var isActivityResumed = false
        private var wasActivityResumedBefore = false
        
        fun setAppRunningState(running: Boolean) {
            isAppAlreadyRunning = running
        }
        
        fun isAppAlreadyRunning(): Boolean {
            // ✅ App está em execução se:
            // 1. Estava rodando antes E
            // 2. A atividade já foi resumida pelo menos uma vez OU está atualmente resumida
            return isAppAlreadyRunning && (wasActivityResumedBefore || isActivityResumed)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        viewModel.handleSignInResult(task)
    }
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Verificar se a permissão foi concedida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permissão de gerenciamento concedida
            }
        }
    }
    
    private val writePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permissão de escrita concedida
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permissão de notificação concedida
        }
    }

    private val jsonFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Obter o nome do arquivo
            val fileName = getFileName(uri)
            
            // Abrir tela de configuração
            viewModel.openJsonConfigScreen(fileName, it)
        }
    }
    
    private fun getFileName(uri: Uri): String {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex) ?: "arquivo.json"
                    } else {
                        "arquivo.json"
                    }
                } else {
                    "arquivo.json"
                }
            } ?: "arquivo.json"
        } catch (e: Exception) {
            "arquivo.json"
        }
    }


    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }



    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun openJsonFilePicker() {
        jsonFilePickerLauncher.launch("application/json")
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()



        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)
        
        // ✅ Verificar se é um retorno via widget ou ícone do app
        val isAppAlreadyRunning = isAppAlreadyRunning()
        if (isAppAlreadyRunning) {
            // ✅ Se o app já está em execução, pular animação de carregamento
            viewModel.skipLoadingAnimation()
        }

        // Removido: requestIgnoreBatteryOptimizations() - agora será solicitado contextualmente
        
        // Configurar callback para o botão de voltar do sistema
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.uiState.value
                when {
                    state.activityToEdit != null -> viewModel.closeCreateActivityModal()
                    state.isSidebarOpen -> viewModel.closeSidebar()
                    state.isCalendarVisualizationSettingsOpen -> viewModel.closeCalendarVisualizationSettings()
                    state.isSettingsScreenOpen -> viewModel.closeSettingsScreen()
                    state.isSearchScreenOpen -> viewModel.closeSearchScreen()
                    state.isChartScreenOpen -> viewModel.closeChartScreen()
                    state.isNotesScreenOpen -> viewModel.closeNotesScreen()
                    state.isAlarmsScreenOpen -> viewModel.closeAlarmsScreen()
                    state.isTrashScreenOpen -> viewModel.closeTrashScreen()
                    state.isBackupScreenOpen -> viewModel.closeBackupScreen()
                    state.isCompletedTasksScreenOpen -> viewModel.closeCompletedTasksScreen()
            state.isPrintCalendarScreenOpen -> viewModel.closePrintCalendarScreen()
                    state.isJsonConfigScreenOpen -> viewModel.closeJsonConfigScreen()
                    else -> finish()
                }
            }
        })

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            var isThemeLoaded by remember { mutableStateOf(false) }
            var showOnboarding by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                viewModel.uiState.first()
                isThemeLoaded = true
            }

            LaunchedEffect(uiState.signInIntent) {
                uiState.signInIntent?.let {
                    googleSignInLauncher.launch(it)
                    viewModel.onSignInLaunched() // Consome o evento
                }
            }

            if (isThemeLoaded) {
        TheBigCalendarTheme(
            darkTheme = when (uiState.theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                else -> isSystemInDarkTheme()
            },
            dynamicColor = true, // Sempre permitir cores dinâmicas
            pureBlack = uiState.pureBlackTheme && when (uiState.theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                else -> isSystemInDarkTheme()
            },
            primaryColorHex = uiState.primaryColor
        ) {
                    if (showOnboarding) {
                        OnboardingFlow(
                            onComplete = {
                                showOnboarding = false
                            },
                            onGoogleSignIn = {
                                // Usar a função de login do Google existente
                                viewModel.onSignInClicked()
                            },
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                            onRequestStoragePermission = {
                                // Usar a mesma lógica da tela de backups
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    // Android 11+: Solicitar permissão de gerenciamento de arquivos
                                    if (!Environment.isExternalStorageManager()) {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        try {
                                            storagePermissionLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            // Fallback para configurações gerais do app
                                            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", packageName, null)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            storagePermissionLauncher.launch(fallbackIntent)
                                        }
                                    }
                                } else {
                                    // Android < 11: Solicitar permissão de escrita
                                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                        writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            }
                        )
                    } else {
                        // Mostrar loading até o calendário estar carregado
                        if (!uiState.isCalendarLoaded) {
                            // Tela de loading com barra de progresso reta
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.foundation.layout.Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                                ) {
                                    // Título
                                    androidx.compose.material3.Text(
                                        text = stringResource(id = R.string.main_loading_appointments),
                                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // Controlar o progresso da animação
                                    var progress by androidx.compose.runtime.remember { 
                                        androidx.compose.runtime.mutableStateOf(0f) 
                                    }
                                    
                                    // Iniciar animação quando a tela aparece
                                    androidx.compose.runtime.LaunchedEffect(Unit) {
                                        kotlinx.coroutines.delay(100) // Pequeno delay para garantir que a tela esteja pronta
                                        progress = 1f // Anima de 0 para 1
                                    }
                                    
                                    // Animação fluída do progresso
                                    val animatedProgress = androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = progress,
                                        animationSpec = androidx.compose.animation.core.tween(
                                            durationMillis = 1200, // 1 segundo de animação
                                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                                        ),
                                        label = "loading_animation"
                                    )
                                    
                                    // Barra de progresso reta com animação fluída
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = animatedProgress.value,
                                        modifier = androidx.compose.ui.Modifier
                                            .width(200.dp)
                                            .height(6.dp),
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                        trackColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    
                                    // Texto de progresso em porcentagem com animação
                                    androidx.compose.material3.Text(
                                        text = stringResource(id = R.string.main_loading_progress, (animatedProgress.value * 100).toInt()),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            when {
                        uiState.isJsonConfigScreenOpen -> {
                            JsonConfigScreen(
                                fileName = uiState.selectedJsonFileName,
                                onBackClick = { viewModel.closeJsonConfigScreen() },
                                onSaveClick = { title, color, jsonContent -> viewModel.saveJsonConfig(title, color, jsonContent) },
                                onSelectFileClick = { openJsonFilePicker() }
                            )
                        }
                        uiState.isCompletedTasksScreenOpen -> {
                            CompletedTasksScreen(
                                onBackClick = { viewModel.closeCompletedTasksScreen() },
                                completedActivities = uiState.completedActivities,
                                onBackPressedDispatcher = onBackPressedDispatcher,
                                onDeleteCompletedActivity = { activityId ->
                                    viewModel.deleteCompletedActivity(activityId)
                                }
                            )
                        }
                        uiState.isPrintCalendarScreenOpen -> {
                            PrintCalendarScreen(
                                uiState = uiState,
                                onNavigateBack = { viewModel.closePrintCalendarScreen() },
                                onGeneratePdf = { printOptions, onPdfGenerated ->
                                    viewModel.generateCalendarPdf(printOptions, onPdfGenerated)
                                }
                            )
                        }
                        uiState.isSearchScreenOpen -> {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.closeSearchScreen() },
                                onSearchResultClick = { result -> viewModel.onSearchResultClick(result) },
                                onBackPressedDispatcher = onBackPressedDispatcher
                            )
                        }
                        uiState.isTrashScreenOpen -> {
                            TrashScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.closeTrashScreen() }
                            )
                        }
                        uiState.isChartScreenOpen -> {
                            ChartScreen(
                                onBackClick = { viewModel.closeChartScreen() },
                                activities = uiState.activities,
                                completedActivities = uiState.completedActivities,
                                last7DaysData = viewModel.getLast7DaysCompletedTasksData(),
                                lastYearData = viewModel.getLastYearCompletedTasksData(),
                                currentMonth = uiState.displayedYearMonth, // Added this line
                                onNavigateToCompletedTasks = { viewModel.onCompletedTasksClick() },
                                onBackPressedDispatcher = onBackPressedDispatcher
                            )
                        }
                        uiState.isNotesScreenOpen -> {
                            SchedulesScreen(
                                onBackClick = { viewModel.closeNotesScreen() },
                                activities = uiState.activities,
                                onBackPressedDispatcher = onBackPressedDispatcher
                            )
                        }
                        uiState.isAlarmsScreenOpen -> {
                            AlarmsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.closeAlarmsScreen() }
                            )
                        }
                        uiState.isSettingsScreenOpen -> {
                            GeneralSettingsScreen(
                                currentTheme = uiState.theme,
                                onThemeChange = { viewModel.onThemeChange(it) },
                                welcomeName = uiState.welcomeName,
                                onWelcomeNameChange = { newName ->
                                    viewModel.onWelcomeNameChange(newName)
                                },
                                googleAccount = uiState.googleSignInAccount,
                                onSignInClicked = { viewModel.onSignInClicked() },
                                onSignOutClicked = { viewModel.signOut() },
                                isSyncing = uiState.isSyncing,
                                onManualSync = { viewModel.onManualSync() },
                                syncProgress = uiState.syncProgress,
                                onBackClick = { viewModel.closeSettingsScreen() },
                                onImportJsonClick = { viewModel.openJsonConfigScreen() },
                                currentAnimation = uiState.animationType,
                                onAnimationChange = { viewModel.onAnimationTypeChange(it) },
                                sidebarFilterVisibility = uiState.sidebarFilterVisibility,
                                onToggleSidebarFilterVisibility = { filterKey ->
                                    viewModel.toggleSidebarFilterVisibility(filterKey)
                                },
                                currentLanguage = uiState.language,
                                onLanguageChange = { language ->
                                    lifecycleScope.launch {
                                        viewModel.onLanguageChange(language)
                                        recreate()
                                    }
                                },
                                onOpenCalendarVisualization = { viewModel.openCalendarVisualizationSettings() }
                            )
                        }
                        uiState.isCalendarVisualizationSettingsOpen -> {
                            CalendarVisualizationSettingsScreen(
                                onBackClick = { viewModel.closeCalendarVisualizationSettings() }
                            )
                        }
                        uiState.isBackupScreenOpen -> {
                            BackupScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.closeBackupScreen() }
                            )
                        }
                        else -> {
                            CalendarScreen(viewModel)
                        }
                        }
                        
                        // Dialog de permissão de segundo plano contextual
                        if (uiState.showBackgroundPermissionDialog) {
                            com.mss.thebigcalendar.ui.components.BackgroundPermissionDialog(
                                onDismissRequest = { 
                                    viewModel.dismissBackgroundPermissionDialog() 
                                },
                                onAllowPermission = { 
                                    viewModel.requestBackgroundPermission()
                                    requestIgnoreBatteryOptimizations()
                                },
                                onDenyPermission = { 
                                    viewModel.dismissBackgroundPermissionDialog() 
                                }
                            )
                        } else {
                        }
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // ✅ Marcar que a atividade está ativa
        isActivityResumed = true
        // ✅ Marcar que a atividade já foi resumida pelo menos uma vez
        wasActivityResumedBefore = true
    }
    
    override fun onPause() {
        super.onPause()
        // ✅ Marcar que a atividade não está mais ativa
        isActivityResumed = false
    }
    
    override fun onStop() {
        super.onStop()
        // ✅ Marcar que o app ainda está em execução (mas não ativo)
        setAppRunningState(true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // ✅ Marcar que o app não está mais em execução
        setAppRunningState(false)
        isActivityResumed = false
        // ✅ Resetar flag para próximo lançamento
        wasActivityResumedBefore = false
    }
}