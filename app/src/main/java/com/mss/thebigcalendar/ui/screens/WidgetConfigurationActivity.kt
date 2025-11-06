package com.mss.thebigcalendar.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.ui.theme.TheBigCalendarTheme

class WidgetConfigurationActivity : ComponentActivity() {
    
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)
        
        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        
        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        setContent {
            TheBigCalendarTheme {
                WidgetConfigurationScreen(
                    appWidgetId = appWidgetId,
                    onSaveConfiguration = { transparency -> saveWidgetConfiguration(transparency) },
                    onCancelConfiguration = { cancelWidgetConfiguration() }
                )
            }
        }
    }
    
    private fun saveWidgetConfiguration(transparency: Float) {
        val context = this
        val prefs = context.getSharedPreferences("widget_prefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putFloat("transparency_$appWidgetId", transparency)
            apply()
        }

        val intent = Intent(this, com.mss.thebigcalendar.widget.GreetingWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = intArrayOf(appWidgetId)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    
    private fun cancelWidgetConfiguration() {
        setResult(RESULT_CANCELED)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigurationScreen(
    appWidgetId: Int,
    onSaveConfiguration: (Float) -> Unit,
    onCancelConfiguration: () -> Unit
) {
    val context = LocalContext.current
    var transparency by remember { mutableStateOf(1f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.widget_configuration_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelConfiguration) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {

                    IconButton(onClick = { onSaveConfiguration(transparency) }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(id = R.string.save),
                            tint = MaterialTheme.colorScheme.primary
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card principal com informações
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.widget_configuration_welcome),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(id = R.string.widget_configuration_description),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Widget ID display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.widget_id_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appWidgetId.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            


            // Card para a configuração de transparência
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.widget_background_transparency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = transparency,
                        onValueChange = { transparency = it },
                        valueRange = 0f..1f,
                        steps = 10
                    )

                    Text(
                        text = "${(transparency * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botões de ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelConfiguration,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
                
                Button(
                    onClick = { onSaveConfiguration(transparency) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.save))
                }
            }
        }
    }
}
