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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.movieapp.core.ui.theme.MovieAppTheme

data class MovieItemUiModel(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val rating: Double? = null,
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Peek scrolling: 16dp start padding, items spaced by 12dp, end padding 24dp
        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    rating = movie.rating,
                    progressPercent = movie.progressPercent,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MovieRowDarkPreview() {
    val sampleMovies = listOf(
        MovieItemUiModel("1", "Phim 1", null, 8.5, 0.4f),
        MovieItemUiModel("2", "Phim 2", null, 9.0),
        MovieItemUiModel("3", "Phim 3", null, 7.8, 0.8f)
    )
    MovieAppTheme(darkTheme = true) {
        MovieRow(
            title = "Phim Thịnh Hành",
            movies = sampleMovies,
            onMovieClick = {}
        )
    }
}
