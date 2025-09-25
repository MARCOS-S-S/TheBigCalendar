package com.mss.thebigcalendar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog para solicitar permissão de segundo plano contextualmente
 */
@Composable
fun BackgroundPermissionDialog(
    onDismissRequest: () -> Unit,
    onAllowPermission: () -> Unit,
    onDenyPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { 
            Text("🔔 Permissão Necessária") 
        },
        text = { 
            Text(
                "Para que suas notificações funcionem corretamente, o app precisa de permissão para trabalhar em segundo plano.\n\n" +
                "Isso permite que você receba lembretes mesmo quando o app não estiver aberto."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onAllowPermission
            ) {
                Text("Permitir")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDenyPermission
            ) {
                Text("Agora não")
            }
        }
    )
}

