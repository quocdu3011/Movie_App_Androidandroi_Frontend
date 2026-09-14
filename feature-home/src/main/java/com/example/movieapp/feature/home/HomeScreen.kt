package com.example.movieapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.MovieItemUiModel
import com.example.movieapp.core.ui.component.MovieRow
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.HomeSection
import com.example.movieapp.domain.model.PersonalizedHome

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMovieClick: (movieId: String) -> Unit,
    onContinueWatchingClick: (movieId: String, playableId: String, sourceItemId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingIndicator()
            }
            is HomeUiState.Error -> {
                ErrorView(
                    message = state.message,
                    requestId = state.requestId,
                    onRetry = { viewModel.refresh() }
                )
            }
            is HomeUiState.Public -> {
                PublicHomeContent(
                    catalogHome = state.catalogHome,
                    onMovieClick = onMovieClick
                )
            }
            is HomeUiState.Personalized -> {
                PersonalizedHomeContent(
                    personalizedHome = state.personalizedHome,
                    onMovieClick = onMovieClick,
                    onContinueWatchingClick = onContinueWatchingClick
                )
            }
        }
    }
}

@Composable
private fun PublicHomeContent(
    catalogHome: CatalogHome,
    onMovieClick: (movieId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderTitle(title = "Trang Chủ")
        }

        if (catalogHome.newReleases.isNotEmpty()) {
            item {
                MovieRow(
                    title = "Mới phát hành",
                    movies = catalogHome.newReleases.map { MovieItemUiModel(it.id, it.title, it.posterUrl) },
                    onMovieClick = { onMovieClick(it.id) }
                )
            }
        }

        if (catalogHome.topRated.isNotEmpty()) {
            item {
                MovieRow(
                    title = "Đánh giá cao",
                    movies = catalogHome.topRated.map { MovieItemUiModel(it.id, it.title, it.posterUrl) },
                    onMovieClick = { onMovieClick(it.id) }
                )
            }
        }

        if (catalogHome.trending.isNotEmpty()) {
            item {
                MovieRow(
                    title = "Xu hướng",
                    movies = catalogHome.trending.map { MovieItemUiModel(it.id, it.title, it.posterUrl) },
                    onMovieClick = { onMovieClick(it.id) }
                )
            }
        }
    }
}

@Composable
private fun PersonalizedHomeContent(
    personalizedHome: PersonalizedHome,
    onMovieClick: (movieId: String) -> Unit,
    onContinueWatchingClick: (movieId: String, playableId: String, sourceItemId: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HeaderTitle(title = "Dành Cho Bạn")
        }

        items(personalizedHome.sections) { section ->
            when (section) {
                is HomeSection.ContinueWatching -> {
                    if (section.items.isNotEmpty()) {
                        ContinueWatchingRow(
                            items = section.items,
                            onItemClick = onContinueWatchingClick
                        )
                    }
                }
                is HomeSection.CatalogNewReleases -> {
                    if (section.items.isNotEmpty()) {
                        MovieRow(
                            title = "Mới phát hành",
                            movies = section.items.map { MovieItemUiModel(it.id, it.title, it.posterUrl) },
                            onMovieClick = { onMovieClick(it.id) }
                        )
                    }
                }
                is HomeSection.FallbackNewReleases -> {
                    if (section.items.isNotEmpty()) {
                        MovieRow(
                            title = "Có thể bạn thích",
                            movies = section.items.map { MovieItemUiModel(it.id, it.title, it.posterUrl) },
                            onMovieClick = { onMovieClick(it.id) }
                        )
                    }
                }
                is HomeSection.Other -> {
                    if (section.items.isNotEmpty()) {
                        MovieRow(
                            title = section.type,
                            movies = section.items.map { MovieItemUiModel(it.id, it.title, it.posterUrl) },
                            onMovieClick = { onMovieClick(it.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderTitle(title: String) {
    Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
private fun ContinueWatchingRow(
    items: List<HistoryItem>,
    onItemClick: (movieId: String, playableId: String, sourceItemId: String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Tiếp tục xem",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { historyItem ->
                ContinueWatchingItemCard(
                    historyItem = historyItem,
                    onClick = {
                        onItemClick(
                            historyItem.movieId,
                            historyItem.playableId,
                            historyItem.sourceItemId
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingItemCard(
    historyItem: HistoryItem,
    onClick: () -> Unit
) {
    val movie = historyItem.movie
    val title = movie?.title ?: "Phim"
    val imageUrl = movie?.backdropUrl ?: movie?.posterUrl

    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val progressPercent = if (historyItem.durationSeconds > 0) {
                    (historyItem.positionSeconds.toFloat() / historyItem.durationSeconds.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
