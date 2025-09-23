package com.mss.thebigcalendar.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.JsonHoliday

@Composable
fun JsonHolidayInfoDialog(
    jsonHoliday: JsonHoliday,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Debug log
    android.util.Log.d("JsonHolidayInfoDialog", "🔍 Dialog Debug:")
    android.util.Log.d("JsonHolidayInfoDialog", "  📋 Name: ${jsonHoliday.name}")
    android.util.Log.d("JsonHolidayInfoDialog", "  🔗 Wikipedia Link: ${jsonHoliday.wikipediaLink}")
    android.util.Log.d("JsonHolidayInfoDialog", "  📝 Summary: ${jsonHoliday.summary}")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = jsonHoliday.name)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = jsonHoliday.summary ?: stringResource(id = R.string.no_information_available))
                Spacer(modifier = Modifier.height(16.dp))
                jsonHoliday.wikipediaLink?.let {
                    android.util.Log.d("JsonHolidayInfoDialog", "  ✅ Wikipedia link encontrado: $it")
                    TextButton(onClick = { 
                        android.util.Log.d("JsonHolidayInfoDialog", "  🚀 Abrindo Wikipedia: $it")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(id = R.string.open_in_wikipedia))
                    }
                } ?: run {
                    android.util.Log.d("JsonHolidayInfoDialog", "  ❌ Wikipedia link é null")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}
