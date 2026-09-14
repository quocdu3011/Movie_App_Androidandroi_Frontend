package com.example.movieapp.core.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.movieapp.core.ui.theme.MovieAppTheme

data class MovieItemUiModel(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val progressPercent: Float? = null
)

@Composable
fun MovieRow(
    title: String,
    movies: List<MovieItemUiModel>,
    onMovieClick: (MovieItemUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    progressPercent = movie.progressPercent,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun MovieRowLightPreview() {
    val sampleMovies = listOf(
        MovieItemUiModel("1", "Phim 1", null, 0.4f),
        MovieItemUiModel("2", "Phim 2", null),
        MovieItemUiModel("3", "Phim 3", null, 0.8f)
    )
    MovieAppTheme(darkTheme = false) {
        MovieRow(
            title = "Phim Thịnh Hành",
            movies = sampleMovies,
            onMovieClick = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MovieRowDarkPreview() {
    val sampleMovies = listOf(
        MovieItemUiModel("1", "Phim 1", null, 0.4f),
        MovieItemUiModel("2", "Phim 2", null),
        MovieItemUiModel("3", "Phim 3", null, 0.8f)
    )
    MovieAppTheme(darkTheme = true) {
        MovieRow(
            title = "Phim Thịnh Hành",
            movies = sampleMovies,
            onMovieClick = {}
        )
    }
}
