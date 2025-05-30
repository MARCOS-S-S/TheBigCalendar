package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // Importa tudo de layout
import androidx.compose.foundation.lazy.LazyRow // Para a lista de cores horizontal
import androidx.compose.foundation.lazy.items // Para a lista de cores horizontal
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.* // Importa tudo de runtime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType

// Lista de cores padrão
val defaultTaskColorsHex = listOf(
    "#F43F5E", "#EF4444", // Rosa, Vermelho
    "#F97316", "#EAB308", // Laranja, Amarelo
    "#22C55E", "#10B981", // Verde Lima, Verde Esmeralda
    "#0EA5E9", "#3B82F6", // Azul Céu, Azul Padrão
    "#8B5CF6", "#A855F7", // Roxo, Púrpura
    "#64748B", "#475569"  // Cinza Ardósia, Cinza Azulado
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityModal(
    activityToEdit: Activity?,
    onDismissRequest: () -> Unit,
    onSaveActivity: (Activity) -> Unit
) {
    val currentActivity = activityToEdit ?: return // Não deve ser nulo devido à lógica do ViewModel

    var title by remember(currentActivity.id) { mutableStateOf(currentActivity.title) }
    // ATUALIZADO: Estado para a cor selecionada, inicializado com a cor da atividade
    var selectedColorHex by remember(currentActivity.id) { mutableStateOf(currentActivity.categoryColor) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentActivity.id) {
        if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
            focusRequester.requestFocus()
            // Se for uma nova tarefa, e a cor padrão não estiver na lista, seleciona a primeira da lista
            if (!defaultTaskColorsHex.contains(selectedColorHex)) {
                selectedColorHex = defaultTaskColorsHex.firstOrNull() ?: currentActivity.categoryColor
            }
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
                    label = { Text("Nome da Tarefa") }, // Ou "Nome do Evento"
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // NOVO: Seletor de Cores
                Text("Cor:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(defaultTaskColorsHex) { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        ColorSwatch(
                            color = color,
                            isSelected = colorHex == selectedColorHex,
                            onClick = { selectedColorHex = colorHex }
                        )
                    }
                }
                // Aqui você adicionaria mais campos no futuro (descrição, data, hora, etc.)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedActivity = currentActivity.copy(
                        title = title.trim(),
                        categoryColor = selectedColorHex // ATUALIZADO: Salva a cor selecionada
                    )
                    if (updatedActivity.title.isNotBlank()) {
                        onSaveActivity(updatedActivity)
                        onDismissRequest()
                    }
                },
                enabled = title.isNotBlank()
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

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface( // Usando Surface para aplicar borda condicionalmente
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = color,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.outline) // Destaque para cor selecionada
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    ) {
        // O conteúdo do Box é opcional, o Surface já tem a cor.
        // Box(modifier = Modifier.fillMaxSize())
    }
}