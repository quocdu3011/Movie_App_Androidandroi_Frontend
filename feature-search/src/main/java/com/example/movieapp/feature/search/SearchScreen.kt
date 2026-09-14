package com.example.movieapp.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.MovieCard
import com.example.movieapp.core.ui.theme.DarkBackground
import com.example.movieapp.core.ui.theme.DarkSurface
import com.example.movieapp.core.ui.theme.PrimaryCoral
import com.example.movieapp.core.ui.theme.TextPrimaryDark
import com.example.movieapp.core.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                val totalItems = gridState.layoutInfo.totalItemsCount
                if (lastIndex != null && totalItems > 0 && lastIndex >= totalItems - 6) {
                    viewModel.loadMore()
                }
            }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text("Tìm kiếm phim...", color = TextSecondaryDark) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = TextSecondaryDark)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = PrimaryCoral,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            FilterRow(
                filter = filter,
                onFilterChanged = viewModel::onFilterChanged
            )

            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nhập từ khóa hoặc chọn bộ lọc để tìm kiếm",
                            fontSize = 14.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                is SearchUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is SearchUiState.History -> {
                    SearchHistorySection(
                        queries = state.queries,
                        onQueryClick = viewModel::onHistoryItemClick,
                        onRemoveQuery = viewModel::onRemoveHistoryItem,
                        onClearAll = viewModel::onClearHistory
                    )
                }

                is SearchUiState.Success -> {
                    if (state.movies.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Không tìm thấy kết quả phù hợp",
                                fontSize = 14.sp,
                                color = TextSecondaryDark
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            state = gridState,
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.movies, key = { it.id }) { movie ->
                                MovieCard(
                                    title = movie.title,
                                    posterUrl = movie.posterUrl,
                                    rating = movie.averageRating,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = PrimaryCoral)
                                    }
                                }
                            }
                        }
                    }
                }

                is SearchUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = {
                            if (query.isNotBlank()) viewModel.onQueryChanged(query)
                            else viewModel.onFilterChanged(filter)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(
    filter: SearchFilter,
    onFilterChanged: (SearchFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val types = listOf(null to "Tất cả", "movie" to "Phim lẻ", "series" to "Phim bộ")
        types.forEach { (typeVal, label) ->
            val isSelected = filter.type == typeVal
            FilterChip(
                selected = isSelected,
                onClick = { onFilterChanged(filter.copy(type = if (filter.type == typeVal) null else typeVal)) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryCoral,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurface,
                    labelColor = TextSecondaryDark
                ),
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(
    queries: List<String>,
    onQueryClick: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryCoral)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lịch sử tìm kiếm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
            if (queries.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Xóa tất cả", color = PrimaryCoral)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (queries.isEmpty()) {
            Text("Chưa có lịch sử tìm kiếm", fontSize = 14.sp, color = TextSecondaryDark)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                queries.forEach { item ->
                    AssistChip(
                        onClick = { onQueryClick(item) },
                        label = { Text(item, color = TextPrimaryDark) },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(containerColor = DarkSurface),
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Xóa mục",
                                tint = TextSecondaryDark,
                                modifier = Modifier
                                    .clickable { onRemoveQuery(item) }
                                    .padding(2.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

