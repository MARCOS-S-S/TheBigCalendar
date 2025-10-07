package com.mss.thebigcalendar.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.CalendarUiState
import com.mss.thebigcalendar.data.model.ViewMode
import com.mss.thebigcalendar.ui.components.MonthlyCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintCalendarScreen(
    uiState: CalendarUiState,
    onNavigateBack: () -> Unit,
    onGeneratePdf: (PrintOptions) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var includeTasks by remember { mutableStateOf(true) }
    var includeHolidays by remember { mutableStateOf(true) }
    var includeSaintDays by remember { mutableStateOf(true) }
    var includeEvents by remember { mutableStateOf(true) }
    var includeBirthdays by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }
    var includeMoonPhases by remember { mutableStateOf(false) }
    var includeCompletedTasks by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf(PageOrientation.PORTRAIT) }
    var pageSize by remember { mutableStateOf(PageSize.A4) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.print_calendar)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(id = R.string.previous_month))
                }
                Text(
                    text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(id = R.string.next_month))
                }
            }

            HorizontalDivider()

            // Content Options
            Text(
                text = stringResource(id = R.string.content_options),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = includeTasks,
                    onClick = { includeTasks = !includeTasks },
                    label = { Text(stringResource(id = R.string.tasks)) }
                )
                FilterChip(
                    selected = includeHolidays,
                    onClick = { includeHolidays = !includeHolidays },
                    label = { Text(stringResource(id = R.string.national_holidays)) }
                )
                FilterChip(
                    selected = includeSaintDays,
                    onClick = { includeSaintDays = !includeSaintDays },
                    label = { Text(stringResource(id = R.string.catholic_saint_days)) }
                )
                FilterChip(
                    selected = includeEvents,
                    onClick = { includeEvents = !includeEvents },
                    label = { Text(stringResource(id = R.string.events)) }
                )
                FilterChip(
                    selected = includeBirthdays,
                    onClick = { includeBirthdays = !includeBirthdays },
                    label = { Text(stringResource(id = R.string.birthday)) }
                )
                FilterChip(
                    selected = includeNotes,
                    onClick = { includeNotes = !includeNotes },
                    label = { Text(stringResource(id = R.string.notes_title)) }
                )
                FilterChip(
                    selected = includeMoonPhases,
                    onClick = { includeMoonPhases = !includeMoonPhases },
                    label = { Text(stringResource(id = R.string.moon_phases_filter)) }
                )
                FilterChip(
                    selected = includeCompletedTasks,
                    onClick = { includeCompletedTasks = !includeCompletedTasks },
                    label = { Text(stringResource(id = R.string.completed_tasks_filter)) }
                )
            }

            HorizontalDivider()

            // Print Settings
            Text(
                text = stringResource(id = R.string.print_settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.orientation), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = orientation == PageOrientation.PORTRAIT,
                        onClick = { orientation = PageOrientation.PORTRAIT },
                        label = { Text(stringResource(id = R.string.portrait)) }
                    )
                    FilterChip(
                        selected = orientation == PageOrientation.LANDSCAPE,
                        onClick = { orientation = PageOrientation.LANDSCAPE },
                        label = { Text(stringResource(id = R.string.landscape)) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(id = R.string.page_size), style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pageSize == PageSize.A4,
                        onClick = { pageSize = PageSize.A4 },
                        label = { Text("A4") }
                    )
                    FilterChip(
                        selected = pageSize == PageSize.A3,
                        onClick = { pageSize = PageSize.A3 },
                        label = { Text("A3") }
                    )
                }
            }

            HorizontalDivider()

            // Preview
            Text(
                text = stringResource(id = R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.pdf_preview_placeholder))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Generate PDF Button
            Button(
                onClick = {
                    val options = PrintOptions(
                        selectedMonth = selectedMonth,
                        includeTasks = includeTasks,
                        includeHolidays = includeHolidays,
                        includeSaintDays = includeSaintDays,
                        includeEvents = includeEvents,
                        includeBirthdays = includeBirthdays,
                        includeNotes = includeNotes,
                        includeMoonPhases = includeMoonPhases,
                        includeCompletedTasks = includeCompletedTasks,
                        orientation = orientation,
                        pageSize = pageSize
                    )
                    onGeneratePdf(options)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.generate_pdf))
            }
        }
    }
}

data class PrintOptions(
    val selectedMonth: java.time.YearMonth,
    val includeTasks: Boolean,
    val includeHolidays: Boolean,
    val includeSaintDays: Boolean,
    val includeEvents: Boolean,
    val includeBirthdays: Boolean,
    val includeNotes: Boolean,
    val includeMoonPhases: Boolean,
    val includeCompletedTasks: Boolean,
    val orientation: PageOrientation,
    val pageSize: PageSize
)

enum class PageOrientation { PORTRAIT, LANDSCAPE }
enum class PageSize { A4, A3 }