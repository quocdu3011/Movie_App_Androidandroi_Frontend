package com.example.movieapp.feature.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.PrimaryButton
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PlayableItem
import com.example.movieapp.domain.model.SourceItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    movieId: String,
    viewModel: MovieDetailViewModel,
    profileId: String?,
    onPlayClick: (movieId: String, playableId: String, sourceItemId: String, profileId: String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(movieId) {
        viewModel.loadMovieDetail(movieId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            LoadingIndicator()
        } else if (uiState.errorMessage != null) {
            ErrorView(
                message = uiState.errorMessage ?: "Đã xảy ra lỗi",
                requestId = uiState.requestId,
                onRetry = { viewModel.loadMovieDetail(movieId) }
            )
        } else if (uiState.movieDetail != null) {
            val detail = uiState.movieDetail!!
            val movie = detail.movie

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Backdrop Image
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (!movie.backdropUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = movie.backdropUrl,
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Title & Basic Info Section
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = movie.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { viewModel.toggleFavorite() },
                                enabled = profileId != null
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Yêu thích",
                                    tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        if (!movie.originTitle.isNullOrBlank()) {
                            Text(
                                text = movie.originTitle!!,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            movie.releaseYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Text(
                                text = "⭐ ${String.format("%.1f", movie.averageRating)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = movie.accessTier.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!movie.description.isNullOrBlank()) {
                            Text(
                                text = movie.description!!,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Season Selector (if Series)
                if (movie.type == "series") {
                    val seasons = detail.playableItems
                        .mapNotNull { it.seasonNumber }
                        .distinct()
                        .sorted()

                    if (seasons.isNotEmpty()) {
                        item {
                            Text(
                                text = "Chọn mùa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            ScrollableTabRow(
                                selectedTabIndex = seasons.indexOf(uiState.selectedSeasonNumber).coerceAtLeast(0),
                                edgePadding = 16.dp
                            ) {
                                seasons.forEach { seasonNumber ->
                                    Tab(
                                        selected = uiState.selectedSeasonNumber == seasonNumber,
                                        onClick = { viewModel.selectSeason(seasonNumber) },
                                        text = { Text("Mùa $seasonNumber") }
                                    )
                                }
                            }
                        }

                        // Episodes list for selected season
                        val currentSeasonPlayables = detail.playableItems
                            .filter { it.seasonNumber == uiState.selectedSeasonNumber }
                            .sortedBy { it.sortOrder }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Danh sách tập",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentSeasonPlayables) { playable ->
                                    FilterChip(
                                        selected = uiState.selectedPlayableItem?.id == playable.id,
                                        onClick = { viewModel.selectPlayableItem(playable) },
                                        label = { Text(playable.label.ifBlank { "Tập ${playable.episodeNumber ?: playable.sortOrder}" }) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Source/Server Selector
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chọn nguồn phát (Server)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (uiState.availableSources.isEmpty()) {
                        Text(
                            text = "Chưa có nguồn phát khả dụng cho tập này.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.availableSources) { source ->
                                val label = source.serverLabel ?: source.serverKey ?: "Server"
                                FilterChip(
                                    selected = uiState.selectedSourceItem?.id == source.id,
                                    onClick = { viewModel.selectSourceItem(source) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // Play Button
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    val selectedPlayable = uiState.selectedPlayableItem
                    val selectedSource = uiState.selectedSourceItem
                    val canPlay = selectedPlayable != null &&
                            selectedSource != null &&
                            !selectedSource.sourceItemId.isNullOrBlank()

                    PrimaryButton(
                        text = "Phát Phim",
                        onClick = {
                            if (canPlay) {
                                onPlayClick(
                                    movie.id,
                                    selectedPlayable!!.id,
                                    selectedSource!!.sourceItemId!!,
                                    profileId
                                )
                            }
                        },
                        enabled = canPlay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
