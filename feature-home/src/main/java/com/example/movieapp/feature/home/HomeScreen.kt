package com.example.movieapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.movieapp.core.ui.component.MovieItemUiModel
import com.example.movieapp.core.ui.component.MovieRow
import com.example.movieapp.core.ui.theme.DarkBackground
import com.example.movieapp.core.ui.theme.DarkSurface
import com.example.movieapp.core.ui.theme.PrimaryCoral
import com.example.movieapp.core.ui.theme.SecondaryGold
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.HomeSection
import com.example.movieapp.domain.model.Movie
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
            .background(DarkBackground)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingIndicator(modifier = Modifier.fillMaxSize())
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
    val heroMovie = catalogHome.trending.firstOrNull() ?: catalogHome.newReleases.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (heroMovie != null) {
            item {
                HeroBanner(movie = heroMovie, onClick = { onMovieClick(heroMovie.id) })
            }
        }

        if (catalogHome.newReleases.isNotEmpty()) {
            item {
                MovieRow(
                    title = "Mới phát hành",
                    movies = catalogHome.newReleases.map {
                        MovieItemUiModel(it.id, it.title, it.posterUrl, it.averageRating)
                    },
                    onMovieClick = { onMovieClick(it.id) }
                )
            }
        }

        if (catalogHome.topRated.isNotEmpty()) {
            item {
                MovieRow(
                    title = "Đánh giá cao",
                    movies = catalogHome.topRated.map {
                        MovieItemUiModel(it.id, it.title, it.posterUrl, it.averageRating)
                    },
                    onMovieClick = { onMovieClick(it.id) }
                )
            }
        }

        if (catalogHome.trending.isNotEmpty()) {
            item {
                MovieRow(
                    title = "Xu hướng",
                    movies = catalogHome.trending.map {
                        MovieItemUiModel(it.id, it.title, it.posterUrl, it.averageRating)
                    },
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
    val allMovies = personalizedHome.sections.flatMap {
        when (it) {
            is HomeSection.CatalogNewReleases -> it.items
            is HomeSection.FallbackNewReleases -> it.items
            is HomeSection.Other -> it.items
            else -> emptyList()
        }
    }
    val heroMovie = allMovies.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (heroMovie != null) {
            item {
                HeroBanner(movie = heroMovie, onClick = { onMovieClick(heroMovie.id) })
            }
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
                            title = "Dành riêng cho bạn",
                            movies = section.items.map {
                                MovieItemUiModel(it.id, it.title, it.posterUrl, it.averageRating)
                            },
                            onMovieClick = { onMovieClick(it.id) }
                        )
                    }
                }
                is HomeSection.FallbackNewReleases -> {
                    if (section.items.isNotEmpty()) {
                        MovieRow(
                            title = "Có thể bạn thích",
                            movies = section.items.map {
                                MovieItemUiModel(it.id, it.title, it.posterUrl, it.averageRating)
                            },
                            onMovieClick = { onMovieClick(it.id) }
                        )
                    }
                }
                is HomeSection.Other -> {
                    if (section.items.isNotEmpty()) {
                        MovieRow(
                            title = section.type,
                            movies = section.items.map {
                                MovieItemUiModel(it.id, it.title, it.posterUrl, it.averageRating)
                            },
                            onMovieClick = { onMovieClick(it.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBanner(
    movie: Movie,
    onClick: () -> Unit
) {
    val backdropUrl = movie.backdropUrl ?: movie.posterUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!backdropUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dark gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.5f),
                                    DarkBackground
                                )
                            )
                        )
                )

                // Title & meta overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 72.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = movie.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (movie.averageRating > 0) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SecondaryGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", movie.averageRating),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        movie.releaseYear?.let { year ->
                            Text(
                                text = "$year",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Floating play button at bottom right edge of banner
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = PrimaryCoral,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 12.dp)
                .size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Phát",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ContinueWatchingRow(
    items: List<HistoryItem>,
    onItemClick: (movieId: String, playableId: String, sourceItemId: String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Tiếp tục xem",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Peek scrolling: 16dp start padding, 12dp item spacing, 24dp end padding
        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
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

    Column(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Coral pink thin progress bar at bottom edge
                val progressPercent = if (historyItem.durationSeconds > 0) {
                    (historyItem.positionSeconds.toFloat() / historyItem.durationSeconds.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent)
                            .background(PrimaryCoral)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
