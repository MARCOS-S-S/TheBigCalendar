package com.mss.thebigcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.Holiday
import com.mss.thebigcalendar.data.model.HolidayType

@Composable
fun HolidaysForSelectedDaySection(
    modifier: Modifier = Modifier,
    holidays: List<Holiday>
) {
    if (holidays.isNotEmpty()) {
        Column(modifier = modifier) {
            Text(
                text = stringResource(id = R.string.holidays),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Red, // Set text color to red
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                holidays.forEach { holiday ->
                    HolidayItem(holiday = holiday)
                }
            }
        }
    }
}

@Composable
fun HolidayItem(
    holiday: Holiday,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = holiday.name, 
            style = MaterialTheme.typography.bodyLarge,
            color = if (holiday.type == HolidayType.NATIONAL) Color.Red else MaterialTheme.colorScheme.onSurface
        )
    }
}
