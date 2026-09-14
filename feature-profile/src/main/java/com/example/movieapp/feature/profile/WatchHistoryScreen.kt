package com.example.movieapp.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.core.common.formatDurationSeconds
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.domain.model.HistoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    onPlayClick: (movieId: String, playableId: String, sourceItemId: String) -> Unit,
    viewModel: WatchHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử xem") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is WatchHistoryUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is WatchHistoryUiState.NoProfileSelected -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Vui lòng chọn hồ sơ để xem lịch sử phát", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                is WatchHistoryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa có lịch sử xem phim nào", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.items, key = { "${it.movieId}_${it.playableId}" }) { item ->
                                HistoryCard(
                                    item = item,
                                    onClick = {
                                        onPlayClick(item.movieId, item.playableId, item.sourceItemId)
                                    }
                                )
                            }
                        }
                    }
                }

                is WatchHistoryUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = viewModel::loadHistory
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    onClick: () -> Unit
) {
    val progress = if (item.durationSeconds > 0) {
        (item.positionSeconds.toFloat() / item.durationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.movie?.title ?: "Phim ID: ${item.movieId}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đã xem: ${item.positionSeconds.formatDurationSeconds()} / ${item.durationSeconds.formatDurationSeconds()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Phát tiếp",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
