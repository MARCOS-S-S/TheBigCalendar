package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.Language
import android.util.Log
import androidx.compose.ui.res.stringResource
import com.mss.thebigcalendar.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    Log.d("LanguageSelectionDialog", "📱 LanguageSelectionDialog criado")
    
    AlertDialog(
        onDismissRequest = {
            Log.d("LanguageSelectionDialog", "❌ Diálogo fechado por dismiss")
            onDismiss()
        },
        title = {
            Text(stringResource(id = R.string.language_selection_title))
        },
        text = {
            Log.d("LanguageSelectionDialog", "📝 Renderizando lista de idiomas")
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(Language.getAvailableLanguages()) { language ->
                    Log.d("LanguageSelectionDialog", "📋 Renderizando idioma: ${language.displayName}")
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                Log.d("LanguageSelectionDialog", "🌐 Idioma clicado: ${language.displayName} (${language.code})")
                                onLanguageSelected(language)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (language == currentLanguage) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = language.flag,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (language == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            if (language == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selecionado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Log.d("LanguageSelectionDialog", "❌ Botão cancelar clicado")
                onDismiss()
            }) {
                Text(stringResource(id = R.string.cancel_button))
            }
        }
    )
}
