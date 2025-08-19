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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType

// Lista de cores padrão
//val defaultTaskColorsHex = listOf(
//    "#F43F5E", "#EF4444", // Rosa, Vermelho
//    "#F97316", "#EAB308", // Laranja, Amarelo
//    "#22C55E", "#10B981", // Verde Lima, Verde Esmeralda
//    "#0EA5E9", "#3B82F6", // Azul Céu, Azul Padrão
//    "#8B5CF6", "#A855F7", // Roxo, Púrpura
//    "#64748B", "#475569"  // Cinza Ardósia, Cinza Azulado
//)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityModal(
    activityToEdit: Activity?,
    onDismissRequest: () -> Unit,
    onSaveActivity: (Activity) -> Unit
) {
    val currentActivity = activityToEdit ?: return

    var title by remember(currentActivity.id) { mutableStateOf(currentActivity.title) }
    // ATUALIZADO: Estado para a cor selecionada, inicializado com a cor da atividade
    var selectedPriority by remember(currentActivity.id) { mutableStateOf(currentActivity.categoryColor) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentActivity.id) {
        if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
            focusRequester.requestFocus()

            // Define as prioridades válidas
            val validPriorities = listOf("1", "2", "3", "4")

            // Se for uma nova tarefa e a prioridade atual não for uma das válidas,
            // define a primeira da lista ("1") como padrão.
            if (selectedPriority !in validPriorities) {
                selectedPriority = validPriorities.first()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                    if (currentActivity.activityType == ActivityType.TASK) stringResource(id = R.string.create_activity_modal_scheduling_for) else stringResource(id = R.string.create_activity_modal_new_event)
                } else {
                    if (currentActivity.activityType == ActivityType.TASK) stringResource(id = R.string.create_activity_modal_edit_task) else stringResource(id = R.string.create_activity_modal_edit_event)
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.create_activity_modal_title)) }, // Ou "Nome do Evento"
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // NOVO: Seletor de Cores
//                Text(stringResource(id = R.string.create_activity_modal_color), style = MaterialTheme.typography.labelMedium)
//                Spacer(modifier = Modifier.height(8.dp))
//                LazyRow(
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    contentPadding = PaddingValues(horizontal = 4.dp)
//                ) {
//                    items(defaultTaskColorsHex) { colorHex ->
//                        val color = Color(android.graphics.Color.parseColor(colorHex))
//                        ColorSwatch(
//                            color = color,
//                            isSelected = colorHex == selectedColorHex,
//                            onClick = { selectedColorHex = colorHex }
//                        )
//                    }
//                }
                PrioritySelector(
                    selectedPriority = selectedPriority,
                    onPrioritySelected = { selectedPriority = it }
                )

                // Adicionar mais campos no futuro (descrição, data, hora, etc.)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedActivity = currentActivity.copy(
                        title = title.trim(),
                        categoryColor = selectedPriority // ATUALIZADO: Salva a cor selecionada
                    )
                    if (updatedActivity.title.isNotBlank()) {
                        onSaveActivity(updatedActivity)
                        onDismissRequest()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(id = R.string.create_activity_modal_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.create_activity_modal_cancel))
            }
        }
    )
}

@Composable
fun PrioritySelector(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val priorities = mapOf(
        "1" to Color.White,
        "2" to Color.Blue,
        "3" to Color.Yellow,
        "4" to Color.Red
    )

    Column(modifier = modifier) {
        Text("Prioridade:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            priorities.forEach { (priority, color) ->
                val isSelected = selectedPriority == priority
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { onPrioritySelected(priority) },
                    shape = CircleShape,
                    color = color,
                    border = if (isSelected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    } else if (color == Color.White) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    } else {
                        null
                    }
                ) {}
            }
        }
    }
}

//@Composable
//private fun ColorSwatch(
//    color: Color,
//    isSelected: Boolean,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Surface( // Usando Surface para aplicar borda condicionalmente
//        modifier = modifier
//            .size(32.dp)
//            .clip(CircleShape)
//            .clickable(onClick = onClick),
//        shape = CircleShape,
//        color = color,
//        border = if (isSelected) {
//            BorderStroke(2.dp, MaterialTheme.colorScheme.outline) // Destaque para cor selecionada
//        } else {
//            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
//        }
//    ) {
//        // O conteúdo do Box é opcional, o Surface já tem a cor.
//        // Box(modifier = Modifier.fillMaxSize())
//    }
//}