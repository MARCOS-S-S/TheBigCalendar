package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityModal(
    activityToEdit: Activity?, // Se null, estamos criando uma nova tarefa/evento
    onDismissRequest: () -> Unit,
    onSaveActivity: (Activity) -> Unit
) {
    // Se activityToEdit for nulo, significa que é uma nova atividade.
    // O ViewModel já nos envia um 'activityToEdit' com id="new" e activityType pré-definido.
    val currentActivity = activityToEdit ?: return // Não deveria acontecer se o ViewModel estiver correto

    var title by remember(currentActivity.id) { mutableStateOf(currentActivity.title) }
    val focusRequester = remember { FocusRequester() }

    // Foca o campo de texto quando o modal abre para uma nova tarefa
    LaunchedEffect(currentActivity.id) {
        if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
            focusRequester.requestFocus()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                    if (currentActivity.activityType == ActivityType.TASK) "Nova Tarefa" else "Novo Evento"
                } else {
                    if (currentActivity.activityType == ActivityType.TASK) "Editar Tarefa" else "Editar Evento"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Tarefa") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                // Aqui você adicionaria mais campos no futuro (descrição, data, hora, etc.)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedActivity = currentActivity.copy(
                        title = title.trim()
                        // Outros campos seriam atualizados aqui também
                    )
                    if (updatedActivity.title.isNotBlank()) {
                        onSaveActivity(updatedActivity)
                        onDismissRequest() // Fecha o modal após salvar
                    }
                },
                enabled = title.isNotBlank() // Só permite salvar se o título não estiver vazio
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}