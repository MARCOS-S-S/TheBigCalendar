package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Theme

@Composable
fun GeneralSettingsScreen(
    currentTheme: Theme,
    onThemeChange: (Theme) -> Unit,
    googleAccount: GoogleSignInAccount?,
    onSignInClicked: () -> Unit,
    onSignOutClicked: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = stringResource(id = R.string.sidebar_change_theme),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = when (currentTheme) {
                    Theme.LIGHT -> false
                    Theme.DARK -> true
                    Theme.SYSTEM -> isSystemInDarkTheme()
                },
                onCheckedChange = { isChecked ->
                    onThemeChange(if (isChecked) Theme.DARK else Theme.LIGHT)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Text(
                text = "Conta Google",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            if (googleAccount != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(googleAccount.email ?: "", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onSignOutClicked) {
                        Text("Desconectar")
                    }
                }
            } else {
                Button(onClick = onSignInClicked) {
                    Text("Conectar")
                }
            }
        }
    }
}
