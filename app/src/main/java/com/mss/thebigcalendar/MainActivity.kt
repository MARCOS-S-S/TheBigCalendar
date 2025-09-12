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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.screens.SearchScreen
import com.mss.thebigcalendar.ui.screens.TrashScreen
import com.mss.thebigcalendar.ui.screens.ChartScreen
import com.mss.thebigcalendar.ui.screens.CompletedTasksScreen
import com.mss.thebigcalendar.ui.screens.BackupScreen
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import com.mss.thebigcalendar.ui.onboarding.OnboardingFlow
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CalendarViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)

        requestIgnoreBatteryOptimizations()
        
        // Configurar callback para o botão de voltar do sistema
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.uiState.value
                when {
                    state.activityToEdit != null -> viewModel.closeCreateActivityModal()
                    state.isSidebarOpen -> viewModel.closeSidebar()
                    state.currentSettingsScreen != null -> viewModel.closeSettingsScreen()
                    state.isSearchScreenOpen -> viewModel.closeSearchScreen()
                    state.isTrashScreenOpen -> viewModel.closeTrashScreen()
                    state.isBackupScreenOpen -> viewModel.closeBackupScreen()
                    state.isCompletedTasksScreenOpen -> viewModel.closeCompletedTasksScreen()
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
                    }
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
                        when {
                        uiState.isCompletedTasksScreenOpen -> {
                            CompletedTasksScreen(
                                onBackClick = { viewModel.closeCompletedTasksScreen() },
                                completedActivities = uiState.completedActivities,
                                onBackPressedDispatcher = onBackPressedDispatcher
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
                    }
                }
            }
        }
    }
}