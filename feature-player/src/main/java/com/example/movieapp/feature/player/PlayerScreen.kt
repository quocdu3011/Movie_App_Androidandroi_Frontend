package com.example.movieapp.feature.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.ResumeConfirmDialog

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movieId: String,
    playableId: String,
    sourceItemId: String,
    profileId: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(movieId, playableId, sourceItemId, profileId) {
        viewModel.startPlayback(movieId, playableId, sourceItemId, profileId)
    }

    BackHandler {
        viewModel.finishSession(StopReason.NormalStop)
        onBackClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.session != null && !uiState.showResumeDialog) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.playerManager.getPlayer()
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true
                    }
                },
                update = { playerView ->
                    playerView.player = viewModel.playerManager.getPlayer()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState.showResumeDialog) {
            ResumeConfirmDialog(
                resumePositionSeconds = uiState.resumePositionSeconds,
                onConfirm = { resume ->
                    viewModel.onResumeConfirmed(resume)
                }
            )
        }

        if (uiState.isLoading) {
            LoadingIndicator()
        }

        if (uiState.errorMessage != null && !uiState.showResumeDialog) {
            ErrorView(
                message = uiState.errorMessage ?: "Không thể kết nối",
                onRetry = {
                    if (uiState.canRetryManually) {
                        viewModel.startPlayback(movieId, playableId, sourceItemId, profileId)
                    } else {
                        onBackClick()
                    }
                }
            )
        }
    }
}
