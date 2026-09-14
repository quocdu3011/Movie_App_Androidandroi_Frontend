package com.example.movieapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.MovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onMovieClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh sách yêu thích") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FavoritesUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is FavoritesUiState.NoProfileSelected -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Vui lòng chọn hồ sơ để xem danh sách yêu thích", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                is FavoritesUiState.Success -> {
                    if (state.movies.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa có phim nào trong danh sách yêu thích", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.movies, key = { it.id }) { movie ->
                                MovieCard(
                                    title = movie.title,
                                    posterUrl = movie.posterUrl,
                                    onClick = { onMovieClick(movie.id) }
                                )
                            }
                        }
                    }
                }

                is FavoritesUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = viewModel::loadFavorites
                    )
                }
            }
        }
    }
}
