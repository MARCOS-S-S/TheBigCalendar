package com.mss.thebigcalendar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.mss.thebigcalendar.data.model.SearchResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<SearchResult>,
    onSearchResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pesquisar agendamentos, feriados..."
) {
    var isExpanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                onQueryChange(it)
                isExpanded = it.isNotEmpty()
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.isFocused },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Pesquisar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            isExpanded = false
                            focusManager.clearFocus()
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
            shape = RoundedCornerShape(24.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Dropdown de resultados da pesquisa
        AnimatedVisibility(
            visible = isExpanded && searchResults.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                animationSpec = tween(200),
                initialOffsetY = { -it }
            ),
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(200),
                targetOffsetY = { -it }
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
        ) {
            SearchResultsDropdown(
                searchResults = searchResults,
                onResultClick = { result ->
                    onSearchResultClick(result)
                    onQueryChange("")
                    isExpanded = false
                    focusManager.clearFocus()
                }
            )
        }
    }

    // Foca automaticamente quando o componente é montado
    LaunchedEffect(Unit) {
        if (hasFocus) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun SearchResultsDropdown(
    searchResults: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        LazyColumn(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(searchResults) { result ->
                SearchResultItem(
                    result = result,
                    onClick = { onResultClick(result) }
                )
                if (result != searchResults.last()) {
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (result.date != null) {
                Spacer(modifier = Modifier.height(2.dp))
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
