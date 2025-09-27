package com.mss.thebigcalendar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.xr.compose.testing.isFocused
import com.mss.thebigcalendar.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ParsedRule(
    val freq: String = "hours",
    val interval: Int = 1,
    val selectedDays: Set<Int> = emptySet(),
    val endDate: String = "",
    val maxOccurrences: Int = 10,
    val hasEndDate: Boolean = false,
    val hasMaxOccurrences: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRepetitionScreen(
    onBackClick: () -> Unit,
    onSaveCustomRepetition: (String) -> Unit = {},
    existingRule: String = "",
    modifier: Modifier = Modifier
) {
    // Parse da regra existente para carregar os dados
    val parsedRule = remember(existingRule) { parseExistingRule(existingRule) }
    
    var repetitionType by remember { mutableStateOf(parsedRule.freq) }
    var interval by remember { mutableStateOf(parsedRule.interval) }
    var intervalText by remember { mutableStateOf(parsedRule.interval.toString()) }
    var selectedDays by remember { mutableStateOf(parsedRule.selectedDays) }
    var endDate by remember { mutableStateOf(parsedRule.endDate) }
    var maxOccurrences by remember { mutableStateOf(parsedRule.maxOccurrences) }
    var maxOccurrencesText by remember { mutableStateOf(parsedRule.maxOccurrences.toString()) }
    var hasEndDate by remember { mutableStateOf(parsedRule.hasEndDate) }
    var hasMaxOccurrences by remember { mutableStateOf(parsedRule.hasMaxOccurrences) }
    
    val weekDays = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.repetition_custom),
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tipo de Repetição
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tipo de Repetição",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { repetitionType = "hours" },
                            label = { Text("Horas") },
                            selected = repetitionType == "hours"
                        )
                        FilterChip(
                            onClick = { repetitionType = "days" },
                            label = { Text("Dias") },
                            selected = repetitionType == "days"
                        )
                        FilterChip(
                            onClick = { repetitionType = "weeks" },
                            label = { Text("Semanas") },
                            selected = repetitionType == "weeks"
                        )
                        FilterChip(
                            onClick = { repetitionType = "months" },
                            label = { Text("Meses") },
                            selected = repetitionType == "months"
                        )
                    }
                }
            }
            
            // Intervalo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Repetir a cada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { newValue: String ->
                                intervalText = newValue
                                // Atualizar valor numérico apenas se for válido
                                val numericValue = newValue.toIntOrNull()
                                if (numericValue != null && numericValue > 0) {
                                    interval = numericValue
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            placeholder = { Text("1") }
                        )
                        Text(
                            text = when (repetitionType) {
                                "hours" -> if (interval == 1) "hora" else "horas"
                                "days" -> if (interval == 1) "dia" else "dias"
                                "weeks" -> if (interval == 1) "semana" else "semanas"
                                "months" -> if (interval == 1) "mês" else "meses"
                                else -> "horas"
                            }
                        )
                    }
                }
            }
            
            // Dias da Semana (apenas para repetição semanal)
            if (repetitionType == "weeks") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Dias da Semana",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(weekDays.size) { index ->
                                FilterChip(
                                    onClick = {
                                        selectedDays = if (selectedDays.contains(index)) {
                                            selectedDays - index
                                        } else {
                                            selectedDays + index
                                        }
                                    },
                                    label = { Text(weekDays[index]) },
                                    selected = selectedDays.contains(index)
                                )
                            }
                        }
                    }
                }
            }
            
            // Data de Término
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Data de Término",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = hasEndDate,
                            onCheckedChange = { hasEndDate = it }
                        )
                    }
                    
                    if (hasEndDate) {
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("DD/MM/AAAA") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            // Número Máximo de Ocorrências
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Número Máximo de Ocorrências",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = hasMaxOccurrences,
                            onCheckedChange = { hasMaxOccurrences = it }
                        )
                    }
                    
                    if (hasMaxOccurrences) {
                        OutlinedTextField(
                            value = maxOccurrencesText,
                            onValueChange = { newValue: String ->
                                maxOccurrencesText = newValue
                                // Atualizar valor numérico apenas se for válido
                                val numericValue = newValue.toIntOrNull()
                                if (numericValue != null && numericValue > 0) {
                                    maxOccurrences = numericValue
                                }
                            },
                            label = { Text("Quantidade") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            placeholder = { Text("10") }
                        )
                    }
                }
            }
            
            // Botão Salvar
            Button(
                onClick = {
                    // Aplicar valores padrão se campos estiverem vazios
                    val finalInterval = if (intervalText.isEmpty()) 1 else interval
                    val finalMaxOccurrences = if (maxOccurrencesText.isEmpty()) 10 else maxOccurrences
                    
                    val customRule = buildCustomRecurrenceRule(
                        repetitionType = repetitionType,
                        interval = finalInterval,
                        selectedDays = selectedDays,
                        endDate = if (hasEndDate) endDate else null,
                        maxOccurrences = if (hasMaxOccurrences) finalMaxOccurrences else null
                    )
                    onSaveCustomRepetition(customRule)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar Configuração")
            }
        }
    }
}

/**
 * Constrói uma regra de repetição personalizada baseada nos parâmetros
 */
private fun buildCustomRecurrenceRule(
    repetitionType: String,
    interval: Int,
    selectedDays: Set<Int>,
    endDate: String?,
    maxOccurrences: Int?
): String {
    val freq = when (repetitionType) {
        "hours" -> "HOURLY"
        "days" -> "DAILY"
        "weeks" -> "WEEKLY"
        "months" -> "MONTHLY"
        else -> "DAILY"
    }
    
    val rule = StringBuilder("FREQ=$freq;INTERVAL=$interval")
    
    // Adicionar dias da semana para repetição semanal
    if (repetitionType == "weeks" && selectedDays.isNotEmpty()) {
        val dayStrings = mutableListOf<String>()
        selectedDays.forEach { day ->
            when (day) {
                0 -> dayStrings.add("SU") // Domingo
                1 -> dayStrings.add("MO") // Segunda
                2 -> dayStrings.add("TU") // Terça
                3 -> dayStrings.add("WE") // Quarta
                4 -> dayStrings.add("TH") // Quinta
                5 -> dayStrings.add("FR") // Sexta
                6 -> dayStrings.add("SA") // Sábado
            }
        }
        
        if (dayStrings.isNotEmpty()) {
            rule.append(";BYDAY=${dayStrings.joinToString(",")}")
        }
    }
    
    // Adicionar data de término
    if (endDate != null && endDate != "") {
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val parsedDate = LocalDate.parse(endDate, formatter)
            rule.append(";UNTIL=${parsedDate}")
        } catch (e: Exception) {
            // Ignorar data inválida
        }
    }
    
    // Adicionar número máximo de ocorrências
    if (maxOccurrences != null && maxOccurrences > 0) {
        rule.append(";COUNT=$maxOccurrences")
    }
    
    return rule.toString()
}

/**
 * Faz o parse de uma regra existente para carregar os dados na tela
 */
private fun parseExistingRule(rule: String): ParsedRule {
    if (rule.isEmpty() || !rule.startsWith("FREQ=")) {
        return ParsedRule()
    }
    
    try {
        val parts = rule.split(";")
        val freq = parts.find { it.startsWith("FREQ=") }?.substringAfter("=") ?: "DAILY"
        val interval = parts.find { it.startsWith("INTERVAL=") }?.substringAfter("=")?.toIntOrNull() ?: 1
        val until = parts.find { it.startsWith("UNTIL=") }?.substringAfter("=")
        val count = parts.find { it.startsWith("COUNT=") }?.substringAfter("=")?.toIntOrNull()
        val byDay = parts.find { it.startsWith("BYDAY=") }?.substringAfter("=")
        
        // Converter frequência para formato da UI
        val uiFreq = when (freq) {
            "HOURLY" -> "hours"
            "DAILY" -> "days"
            "WEEKLY" -> "weeks"
            "MONTHLY" -> "months"
            else -> "days"
        }
        
        // Parse dos dias da semana
        val selectedDays = if (byDay != null && byDay.isNotEmpty()) {
            byDay.split(",").mapNotNull { day ->
                when (day.trim()) {
                    "SU" -> 0 // Domingo
                    "MO" -> 1 // Segunda
                    "TU" -> 2 // Terça
                    "WE" -> 3 // Quarta
                    "TH" -> 4 // Quinta
                    "FR" -> 5 // Sexta
                    "SA" -> 6 // Sábado
                    else -> null
                }
            }.toSet()
        } else {
            emptySet()
        }
        
        // Parse da data de término
        val endDate = if (until != null && until.isNotEmpty()) {
            try {
                val parsedDate = LocalDate.parse(until)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                parsedDate.format(formatter)
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }
        
        return ParsedRule(
            freq = uiFreq,
            interval = interval,
            selectedDays = selectedDays,
            endDate = endDate,
            maxOccurrences = count ?: 10,
            hasEndDate = until != null && until.isNotEmpty(),
            hasMaxOccurrences = count != null
        )
    } catch (e: Exception) {
        return ParsedRule()
    }
}
