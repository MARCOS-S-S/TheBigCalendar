package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Holiday
import androidx.compose.ui.res.stringResource

@Composable
fun SaintDaysForSelectedDaySection(
    modifier: Modifier = Modifier,
    saints: List<Holiday>,
    onSaintClick: (Holiday) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.saint_days),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            saints.forEach { saint ->
                TextButton(onClick = { onSaintClick(saint) }) {
                    Text(
                        text = saint.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}