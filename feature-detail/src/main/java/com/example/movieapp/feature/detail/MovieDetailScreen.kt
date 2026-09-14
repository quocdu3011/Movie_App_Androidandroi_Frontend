package com.example.movieapp.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.theme.DarkBackground
import com.example.movieapp.core.ui.theme.DarkSurface
import com.example.movieapp.core.ui.theme.PrimaryCoral
import com.example.movieapp.core.ui.theme.SecondaryGold
import com.example.movieapp.core.ui.theme.TextPrimaryDark
import com.example.movieapp.core.ui.theme.TextSecondaryDark

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

    val activeProfileId = uiState.profileId ?: profileId

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
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
            val selectedPlayable = uiState.selectedPlayableItem
            val selectedSource = uiState.selectedSourceItem
            val sourceItemId = selectedSource?.sourceItemId
            val canPlay = selectedPlayable != null &&
                    selectedSource != null &&
                    !sourceItemId.isNullOrBlank()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Backdrop Image with Gradient Overlay
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        if (!movie.backdropUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = movie.backdropUrl,
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkSurface)
                            )
                        }

                        // Gradient fading into DarkBackground
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            DarkBackground.copy(alpha = 0.6f),
                                            DarkBackground
                                        )
                                    )
                                )
                        )

                        // Favorite Button on top right
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            enabled = activeProfileId != null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (uiState.isFavorite) PrimaryCoral else TextPrimaryDark
                            )
                        }

                        // Floating Circular Play Button
                        if (canPlay && sourceItemId != null) {
                            FloatingActionButton(
                                onClick = {
                                    onPlayClick(
                                        movie.id,
                                        selectedPlayable!!.id,
                                        sourceItemId,
                                        activeProfileId
                                    )
                                },
                                shape = CircleShape,
                                containerColor = PrimaryCoral,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 20.dp, bottom = 10.dp)
                                    .size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Phát phim",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Title & Basic Info Section
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = movie.title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!movie.originTitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = movie.originTitle!!,
                                fontSize = 14.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (movie.averageRating > 0) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = SecondaryGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", movie.averageRating),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            movie.releaseYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    fontSize = 14.sp,
                                    color = TextSecondaryDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            if (movie.accessTier.isNotBlank()) {
                                val isPremium = movie.accessTier.lowercase() == "vip" || movie.accessTier.lowercase() == "premium"
                                Text(
                                    text = if (isPremium) "Premium" else "Miễn phí",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPremium) SecondaryGold else PrimaryCoral
                                )
                            }
                        }

                        if (!movie.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = movie.description!!,
                                fontSize = 14.sp,
                                color = TextSecondaryDark,
                                lineHeight = 20.sp
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
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Chọn mùa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(seasons) { seasonNumber ->
                                    val isSelected = uiState.selectedSeasonNumber == seasonNumber
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectSeason(seasonNumber) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryCoral,
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurface,
                                            labelColor = TextSecondaryDark
                                        ),
                                        label = { Text("Mùa $seasonNumber") }
                                    )
                                }
                            }
                        }

                        // Episodes list for selected season
                        val currentSeasonPlayables = detail.playableItems
                            .filter { it.seasonNumber == uiState.selectedSeasonNumber }
                            .sortedBy { it.sortOrder }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Danh sách tập",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentSeasonPlayables) { playable ->
                                    val isSelected = uiState.selectedPlayableItem?.id == playable.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectPlayableItem(playable) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryCoral,
                                            selectedLabelColor = Color.White,
                                            containerColor = DarkSurface,
                                            labelColor = TextSecondaryDark
                                        ),
                                        label = { Text(playable.label.ifBlank { "Tập ${playable.episodeNumber ?: playable.sortOrder}" }) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Source/Server Selector
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Chọn nguồn phát (Server)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.availableSources.isEmpty()) {
                        Text(
                            text = "Chưa có nguồn phát khả dụng cho tập này.",
                            fontSize = 14.sp,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.availableSources) { source ->
                                val label = source.serverLabel ?: source.serverKey ?: "Server"
                                val isSelected = uiState.selectedSourceItem?.id == source.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectSourceItem(source) },
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
                }
            }
        }
    }
}

