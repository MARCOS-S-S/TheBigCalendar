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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.intl.Locale
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Activity
import com.mss.thebigcalendar.data.model.ActivityType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    var selectedPriority by remember(currentActivity.id) { mutableStateOf(currentActivity.categoryColor) }
    var selectedActivityType by remember(currentActivity.id) { mutableStateOf(currentActivity.activityType) }

    val focusRequester = remember { FocusRequester() }

    val date = LocalDate.parse(currentActivity.date)
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMM", java.util.Locale("pt", "BR"))
    val formattedDate = date.format(formatter)

    LaunchedEffect(currentActivity.id) {
        if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
            focusRequester.requestFocus()

            val validPriorities = listOf("1", "2", "3", "4")

            if (selectedPriority !in validPriorities) {
                selectedPriority = validPriorities.first()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            val titleText = if (currentActivity.id == "new" || currentActivity.id.isBlank()) {
                    if (selectedActivityType == ActivityType.TASK) stringResource(id = R.string.create_activity_modal_scheduling_for) else stringResource(id = R.string.create_activity_modal_new_event)
                } else {
                    if (selectedActivityType == ActivityType.TASK) stringResource(id = R.string.create_activity_modal_edit_task) else stringResource(id = R.string.create_activity_modal_edit_event)
                }
            Text(text = titleText)
            Text(text = "$titleText $formattedDate")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.create_activity_modal_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                PrioritySelector(
                    selectedPriority = selectedPriority,
                    onPrioritySelected = { selectedPriority = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ActivityType.TASK, ActivityType.EVENT).forEach { type ->
                        val isSelected = selectedActivityType == type
                        TextButton(
                            onClick = { selectedActivityType = type },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = if (type == ActivityType.TASK) "Tarefa" else "Evento")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var isRepetitionMenuExpanded by remember { mutableStateOf(false) }
                val repetitionOptions = listOf("Não repetir", "Todos os dias", "Todas as semanas", "Todos os meses", "Todos os anos", "Personalizado...")
                var selectedRepetition by remember { mutableStateOf(repetitionOptions.first()) }

                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRepetitionMenuExpanded = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Repetir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedRepetition,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedRepetition != "Não repetir") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                    DropdownMenu(
                        expanded = isRepetitionMenuExpanded,
                        onDismissRequest = { isRepetitionMenuExpanded = false }
                    ) {
                        repetitionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = option, 
                                        color = if (option == selectedRepetition) MaterialTheme.colorScheme.primary else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    selectedRepetition = option
                                    isRepetitionMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedActivity = currentActivity.copy(
                        title = title.trim(),
                        categoryColor = selectedPriority,
                        activityType = selectedActivityType
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