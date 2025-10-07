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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import android.util.Log
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
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // LaunchedEffect para controlar o estado de geração
    LaunchedEffect(isGeneratingPdf) {
        if (isGeneratingPdf) {
            kotlinx.coroutines.delay(2000)
            isGeneratingPdf = false
        }
    }

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
            // Month Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(id = R.string.previous_month))
                    }
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(id = R.string.next_month))
                    }
                }
            }

            HorizontalDivider()

            // Content Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.content_options),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                }
            }

            HorizontalDivider()

            // Print Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.print_settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Orientation
                    Text(
                        text = stringResource(id = R.string.orientation),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Page Size
                    Text(
                        text = stringResource(id = R.string.page_size),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
            }

            HorizontalDivider()

            // PDF Preview Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.preview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // PDF Preview
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PdfPreviewCalendar(
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
                        }
                    }
                }
            }
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
                    Log.d("PrintCalendar", "🔄 Botão Gerar PDF clicado!")
                    isGeneratingPdf = true
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
                    Log.d("PrintCalendar", "📋 Opções do PDF: $options")
                    onGeneratePdf(options)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeneratingPdf
            ) {
                if (isGeneratingPdf) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Gerando PDF...")
                    }
                } else {
                    Text(stringResource(id = R.string.generate_pdf))
                }
            }
        }
    }
}

@Composable
fun PdfPreviewCalendar(
    selectedMonth: java.time.YearMonth,
    includeTasks: Boolean,
    includeHolidays: Boolean,
    includeSaintDays: Boolean,
    includeEvents: Boolean,
    includeBirthdays: Boolean,
    includeNotes: Boolean,
    includeMoonPhases: Boolean,
    includeCompletedTasks: Boolean,
    orientation: PageOrientation,
    pageSize: PageSize
) {
    // Calcular proporções baseadas na orientação e tamanho
    val aspectRatio = when {
        orientation == PageOrientation.LANDSCAPE -> 1.4f // Paisagem é mais larga
        pageSize == PageSize.A3 -> 1.2f // A3 é maior que A4
        else -> 0.7f // A4 retrato é mais alta
    }
    
    val previewScale = when {
        orientation == PageOrientation.LANDSCAPE -> 0.8f
        pageSize == PageSize.A3 -> 0.9f
        else -> 1.0f
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp, 
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .scale(previewScale),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título do mês (como no PDF)
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grade do calendário (simulando o PDF)
            val firstDayOfMonth = selectedMonth.atDay(1)
            val firstSunday = firstDayOfMonth.minusDays((firstDayOfMonth.dayOfWeek.value % 7).toLong())
            
            // Cabeçalho dos dias da semana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Grade de dias (6 semanas x 7 dias)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (week in 0..5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOfWeek in 0..6) {
                            val currentDate = firstSunday.plusDays(((week * 7) + dayOfWeek).toLong())
                            val isCurrentMonth = currentDate.month == selectedMonth.month
                            val isToday = currentDate.isEqual(java.time.LocalDate.now())
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (orientation == PageOrientation.LANDSCAPE) 24.dp else 32.dp)
                                    .padding(1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxSize(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            isCurrentMonth -> MaterialTheme.colorScheme.surface
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentDate.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Informações sobre o que será incluído
            Text(
                text = buildString {
                    append("Incluindo: ")
                    val items = mutableListOf<String>()
                    if (includeTasks) items.add("Tarefas")
                    if (includeHolidays) items.add("Feriados")
                    if (includeSaintDays) items.add("Santos")
                    if (includeEvents) items.add("Eventos")
                    if (includeBirthdays) items.add("Aniversários")
                    if (includeNotes) items.add("Notas")
                    if (includeMoonPhases) items.add("Fases da Lua")
                    if (includeCompletedTasks) items.add("Tarefas Completadas")
                    append(items.joinToString(", "))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            // Informações de formato com destaque visual
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de orientação
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (orientation == PageOrientation.PORTRAIT) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = if (orientation == PageOrientation.PORTRAIT) "📄 Retrato" else "📄 Paisagem",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (orientation == PageOrientation.PORTRAIT) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                // Indicador de tamanho
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (pageSize == PageSize.A4) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = if (pageSize == PageSize.A4) "📏 A4" else "📏 A3",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pageSize == PageSize.A4) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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