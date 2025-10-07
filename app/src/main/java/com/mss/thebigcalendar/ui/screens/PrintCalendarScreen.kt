package com.mss.thebigcalendar.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.IOException
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
    onGeneratePdf: (PrintOptions, (String) -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
    var hideOtherMonthDays by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfPath by remember { mutableStateOf<String?>(null) }

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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Opção para ocultar dias de outros meses
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ocultar dias de outros meses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = hideOtherMonthDays,
                            onCheckedChange = { hideOtherMonthDays = it }
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
                            .height(200.dp),
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
                            if (generatedPdfPath != null) {
                                // Mostrar mensagem de sucesso e opção para abrir PDF
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "✅ PDF gerado com sucesso!",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text(
                                        text = "Arquivo salvo em:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = generatedPdfPath!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                            try {
                                                // Abrir PDF com visualizador padrão do sistema
                                                val file = File(generatedPdfPath!!)
                                                android.util.Log.d("PrintCalendar", "📁 Tentando abrir arquivo: ${file.absolutePath}")
                                                android.util.Log.d("PrintCalendar", "📁 Arquivo existe: ${file.exists()}")
                                                android.util.Log.d("PrintCalendar", "📁 Arquivo pode ler: ${file.canRead()}")
                                                
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                android.util.Log.d("PrintCalendar", "🔗 URI gerado: $uri")
                                                    
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                
                                                // Criar chooser para permitir ao usuário escolher o app
                                                val chooserIntent = android.content.Intent.createChooser(
                                                    intent,
                                                    "Abrir PDF com"
                                                )
                                                chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                
                                                // Tentar abrir o chooser (o Android já lida com apps disponíveis)
                                                try {
                                                    context.startActivity(chooserIntent)
                                                } catch (e: android.content.ActivityNotFoundException) {
                                                    // Se não há apps disponíveis, mostrar mensagem
                                                    android.util.Log.d("PrintCalendar", "Nenhum app disponível para abrir PDF")
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Nenhum visualizador de PDF encontrado. Instale um app como Adobe Reader ou Google PDF Viewer.",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("PrintCalendar", "Erro ao abrir PDF: ${e.message}", e)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Erro ao abrir PDF: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Abrir PDF")
                                        }
                                        
                                        Button(
                                            onClick = {
                                            try {
                                                // Compartilhar PDF
                                                val file = File(generatedPdfPath!!)
                                                android.util.Log.d("PrintCalendar", "📤 Tentando compartilhar arquivo: ${file.absolutePath}")
                                                android.util.Log.d("PrintCalendar", "📤 Arquivo existe: ${file.exists()}")
                                                android.util.Log.d("PrintCalendar", "📤 Arquivo pode ler: ${file.canRead()}")
                                                
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                android.util.Log.d("PrintCalendar", "🔗 URI gerado para compartilhar: $uri")
                                                    
                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        setType("application/pdf")
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    
                                                    val chooserIntent = android.content.Intent.createChooser(
                                                        shareIntent,
                                                        "Compartilhar PDF"
                                                    )
                                                    chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    context.startActivity(chooserIntent)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("PrintCalendar", "Erro ao compartilhar PDF: ${e.message}", e)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Erro ao compartilhar PDF: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary
                                            )
                                        ) {
                                            Text("Compartilhar")
                                        }
                                    }
                                }
                            } else {
                                // Mostrar mensagem quando não há PDF
                                Text(
                                    text = stringResource(id = R.string.pdf_preview_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
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
                        pageSize = pageSize,
                        hideOtherMonthDays = hideOtherMonthDays
                    )
                    Log.d("PrintCalendar", "📋 Opções do PDF: $options")
                    onGeneratePdf(options) { pdfPath ->
                        generatedPdfPath = pdfPath
                    }
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
    val pageSize: PageSize,
    val hideOtherMonthDays: Boolean
)

enum class PageOrientation { PORTRAIT, LANDSCAPE }
enum class PageSize { A4, A3 }