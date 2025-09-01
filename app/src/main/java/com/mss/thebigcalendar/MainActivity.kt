package com.mss.thebigcalendar

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mss.thebigcalendar.data.model.Theme
import com.mss.thebigcalendar.ui.screens.CalendarScreen
import com.mss.thebigcalendar.ui.screens.SearchScreen
import com.mss.thebigcalendar.ui.screens.TrashScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)
        
        // Configurar callback para o botão de voltar do sistema
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val state = viewModel.uiState.value
                when {
                    state.isSidebarOpen -> viewModel.closeSidebar()
                    state.currentSettingsScreen != null -> viewModel.closeSettingsScreen()
                    state.isSearchScreenOpen -> viewModel.closeSearchScreen()
                    state.isTrashScreenOpen -> viewModel.closeTrashScreen()
                    state.isBackupScreenOpen -> viewModel.closeBackupScreen()
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
                            }
                        )
                    } else {
                        when {
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