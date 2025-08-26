package com.mss.thebigcalendar.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mss.thebigcalendar.R
import com.mss.thebigcalendar.data.model.SearchResult
import com.mss.thebigcalendar.ui.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit,
    onSearchResultClick: (SearchResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery = uiState.searchQuery
    val searchResults = uiState.searchResults
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Campo de pesquisa
            SearchInputField(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    viewModel.onSearchQueryChange(query)
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Resultados da pesquisa
            if (searchResults.isNotEmpty()) {
                Text(
                    text = "Resultados (${searchResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults) { result ->
                        SearchResultItem(
                            result = result,
                            onClick = { 
                                onSearchResultClick(result)
                                onNavigateBack()
                            }
                        )
                        if (result != searchResults.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                // Nenhum resultado encontrado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum resultado encontrado para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Dicas de pesquisa
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Digite para pesquisar",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Eventos, tarefas, feriados, dias de santos...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Foca automaticamente no campo de pesquisa
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Pesquisar agendamentos, feriados...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                    }
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Limpar pesquisa"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
            }
        ),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone baseado no tipo de resultado
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when (result.type) {
                        SearchResult.Type.ACTIVITY -> MaterialTheme.colorScheme.primaryContainer
                        SearchResult.Type.HOLIDAY -> MaterialTheme.colorScheme.secondaryContainer
                        SearchResult.Type.SAINT_DAY -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (result.type) {
                    SearchResult.Type.ACTIVITY -> "📅"
                    SearchResult.Type.HOLIDAY -> "🎉"
                    SearchResult.Type.SAINT_DAY -> "⛪"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Conteúdo do resultado
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (result.date != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(result.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
    return date.format(formatter).replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
}
