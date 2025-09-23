package com.mss.thebigcalendar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mss.thebigcalendar.data.model.JsonCalendar

/**
 * Dialog de confirmação para deletar calendário JSON importado
 */
@Composable
fun DeleteJsonCalendarDialog(
    jsonCalendar: JsonCalendar?,
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (jsonCalendar != null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { 
                Text("🗑️ Remover Calendário") 
            },
            text = { 
                Text(
                    "Tem certeza que deseja remover o calendário \"${jsonCalendar.title}\"?\n\n" +
                    "Todas as datas e eventos deste calendário serão removidos permanentemente."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete
                ) {
                    Text("Remover")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
