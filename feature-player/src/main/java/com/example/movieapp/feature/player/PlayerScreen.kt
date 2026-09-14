package com.example.movieapp.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.example.movieapp.core.player.PlayerState
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.ResumeConfirmDialog
import com.example.movieapp.core.ui.theme.DarkSurface
import com.example.movieapp.core.ui.theme.PrimaryCoral
import com.example.movieapp.core.ui.theme.TextPrimaryDark
import com.example.movieapp.core.ui.theme.TextSecondaryDark
import com.example.movieapp.domain.model.PlayableItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val playerState by viewModel.playerManager.playerState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen Orientation & Fullscreen Immersive System Bars Management
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(movieId, playableId, sourceItemId, profileId) {
        viewModel.startPlayback(movieId, playableId, sourceItemId, profileId)
    }

    BackHandler {
        viewModel.finishSession(StopReason.NormalStop)
        onBackClick()
    }

    // UI Overlay state
    var isControlsVisible by remember { mutableStateOf(true) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }

    // Gesture indicator overlays state
    var brightnessPercent by remember { mutableFloatStateOf(-1f) }
    var volumePercent by remember { mutableFloatStateOf(-1f) }

    // Seek accumulation state for double taps
    var accumulatedSeekSeconds by remember { mutableIntStateOf(0) }
    var seekOverlaySide by remember { mutableStateOf<String?>(null) } // "left" or "right"
    var lastDoubleTapTimestamp by remember { mutableLongStateOf(0L) }

    // Auto-hide controls timer
    LaunchedEffect(isControlsVisible, playerState) {
        if (isControlsVisible && playerState is PlayerState.Playing) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Position & duration tracking for slider
    var currentPosSec by remember { mutableIntStateOf(0) }
    var durationSec by remember { mutableIntStateOf(0) }

    LaunchedEffect(playerState) {
        while (true) {
            currentPosSec = viewModel.playerManager.currentPositionSeconds()
            durationSec = viewModel.playerManager.durationSeconds() ?: 0
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Player View
        if (uiState.session != null && !uiState.showResumeDialog) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = viewModel.playerManager.getPlayer()
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                    }
                },
                update = { playerView ->
                    playerView.player = viewModel.playerManager.getPlayer()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gesture Detection Overlay Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            isControlsVisible = !isControlsVisible
                        },
                        onDoubleTap = { offset ->
                            val isLeft = offset.x < size.width / 2f
                            val now = System.currentTimeMillis()
                            val side = if (isLeft) "left" else "right"

                            if (side == seekOverlaySide && (now - lastDoubleTapTimestamp) < 1200) {
                                accumulatedSeekSeconds += if (isLeft) -10 else 10
                            } else {
                                accumulatedSeekSeconds = if (isLeft) -10 else 10
                                seekOverlaySide = side
                            }
                            lastDoubleTapTimestamp = now

                            viewModel.seekRelative((if (isLeft) -10_000L else 10_000L))

                            coroutineScope.launch {
                                delay(1200)
                                if (System.currentTimeMillis() - lastDoubleTapTimestamp >= 1000) {
                                    seekOverlaySide = null
                                    accumulatedSeekSeconds = 0
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                delay(800)
                                brightnessPercent = -1f
                                volumePercent = -1f
                            }
                        },
                        onDragCancel = {
                            brightnessPercent = -1f
                            volumePercent = -1f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val isLeftHalf = change.position.x < size.width / 2f
                            val activity = context.findActivity()

                            if (isLeftHalf && activity != null) {
                                val window = activity.window
                                val currentBrightness = if (window.attributes.screenBrightness < 0) {
                                    0.5f
                                } else {
                                    window.attributes.screenBrightness
                                }
                                val delta = -dragAmount.y / (size.height * 0.7f)
                                val newBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                                val lp = window.attributes
                                lp.screenBrightness = newBrightness
                                window.attributes = lp
                                brightnessPercent = newBrightness
                            } else {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = -dragAmount.y / (size.height * 0.7f) * maxVol
                                val newVol = (currentVol + delta).coerceIn(0f, maxVol.toFloat()).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                volumePercent = newVol.toFloat() / maxVol.toFloat()
                            }
                        }
                    )
                }
        )

        // Overlay Indicator: Brightness
        if (brightnessPercent >= 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "☀️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(brightnessPercent * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Overlay Indicator: Volume
        if (volumePercent >= 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔊", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(volumePercent * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Overlay Indicator: Double-Tap Seek Accumulation
        if (seekOverlaySide != null) {
            Box(
                modifier = Modifier
                    .align(if (seekOverlaySide == "left") Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 64.dp)
                    .background(PrimaryCoral.copy(alpha = 0.85f), CircleShape)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = if (accumulatedSeekSeconds > 0) "+${accumulatedSeekSeconds}s" else "${accumulatedSeekSeconds}s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        // Custom Player Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Bar: Back Button & Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            viewModel.finishSession(StopReason.NormalStop)
                            onBackClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = uiState.movieTitle.ifBlank { "Đang phát" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (uiState.episodeTitle.isNotBlank()) {
                            Text(
                                text = uiState.episodeTitle,
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Center Controls: Tua lùi 10s | Play/Pause | Tua tới 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tua lùi 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(-10_000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Text(
                            text = "-10s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Play / Pause
                    IconButton(
                        onClick = {
                            if (playerState is PlayerState.Playing) {
                                viewModel.playerManager.pause()
                            } else {
                                viewModel.playerManager.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(PrimaryCoral, CircleShape)
                    ) {
                        if (playerState is PlayerState.Playing) {
                            // Custom Pause Icon (Two parallel bars)
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val barWidth = size.width * 0.28f
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(barWidth, size.height),
                                    cornerRadius = CornerRadius(4f)
                                )
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(size.width - barWidth, 0f),
                                    size = Size(barWidth, size.height),
                                    cornerRadius = CornerRadius(4f)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Phát",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Tua tới 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(10_000L) },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Text(
                            text = "+10s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Bottom Bar Controls & Progress Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Slider & Time Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosSec),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (durationSec > 0) currentPosSec.toFloat() / durationSec.toFloat() else 0f,
                            onValueChange = { percent ->
                                val targetMs = (percent * durationSec * 1000L).toLong()
                                viewModel.playerManager.seekTo(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryCoral,
                                activeTrackColor = PrimaryCoral,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = formatTime(durationSec),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Controls Row Below Progress Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side controls: Tập trước | Tốc độ | Độ phân giải
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Tập trước
                            IconButton(
                                onClick = {
                                    val prev = uiState.previousPlayableItem
                                    if (prev != null) {
                                        viewModel.startPlayback(movieId, prev.id, sourceItemId, profileId)
                                    }
                                },
                                enabled = uiState.previousPlayableItem != null
                            ) {
                                Text(
                                    text = "⏮",
                                    fontSize = 18.sp,
                                    color = if (uiState.previousPlayableItem != null) Color.White else Color.White.copy(alpha = 0.3f)
                                )
                            }

                            // Tốc độ phim
                            TextButton(
                                onClick = { showSpeedSheet = true }
                            ) {
                                Text(
                                    text = "⚡ ${uiState.currentSpeed}x",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Độ phân giải
                            TextButton(
                                onClick = { showQualitySheet = true }
                            ) {
                                Text(
                                    text = "HD ${uiState.currentQuality}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right side controls: Danh sách tập | Tập tiếp theo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Danh sách tập
                            if (uiState.playableItems.isNotEmpty()) {
                                IconButton(
                                    onClick = { showEpisodesSheet = true }
                                ) {
                                    Text(
                                        text = "☰",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Tập tiếp theo
                            IconButton(
                                onClick = {
                                    val next = uiState.nextPlayableItem
                                    if (next != null) {
                                        viewModel.startPlayback(movieId, next.id, sourceItemId, profileId)
                                    }
                                },
                                enabled = uiState.nextPlayableItem != null
                            ) {
                                Text(
                                    text = "⏭",
                                    fontSize = 18.sp,
                                    color = if (uiState.nextPlayableItem != null) Color.White else Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Resume confirm dialog
        if (uiState.showResumeDialog) {
            ResumeConfirmDialog(
                resumePositionSeconds = uiState.resumePositionSeconds,
                onConfirm = { resume ->
                    viewModel.onResumeConfirmed(resume)
                }
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            LoadingIndicator()
        }

        // Error View
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

        // Bottom Sheet: Playback Speed Selection
        if (showSpeedSheet) {
            SpeedSelectionSheet(
                currentSpeed = uiState.currentSpeed,
                onSpeedSelected = { speed -> viewModel.setSpeed(speed) },
                onDismiss = { showSpeedSheet = false }
            )
        }

        // Bottom Sheet: Quality Selection
        if (showQualitySheet) {
            QualitySelectionSheet(
                availableQualities = uiState.availableQualities,
                currentQuality = uiState.currentQuality,
                onQualitySelected = { quality -> viewModel.setQuality(quality) },
                onDismiss = { showQualitySheet = false }
            )
        }

        // Bottom Sheet: Episodes List Selection
        if (showEpisodesSheet) {
            EpisodesSelectionSheet(
                playableItems = uiState.playableItems,
                currentPlayableId = uiState.currentPlayableId,
                onEpisodeSelected = { item ->
                    showEpisodesSheet = false
                    viewModel.startPlayback(movieId, item.id, sourceItemId, profileId)
                },
                onDismiss = { showEpisodesSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Tốc độ phát",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            speeds.forEach { speed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSpeedSelected(speed)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${speed}x",
                        color = if (currentSpeed == speed) PrimaryCoral else TextPrimaryDark,
                        fontWeight = if (currentSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySelectionSheet(
    availableQualities: List<String>,
    currentQuality: String,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Độ phân giải",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            availableQualities.forEach { quality ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onQualitySelected(quality)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality,
                        color = if (currentQuality == quality) PrimaryCoral else TextPrimaryDark,
                        fontWeight = if (currentQuality == quality) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodesSelectionSheet(
    playableItems: List<PlayableItem>,
    currentPlayableId: String,
    onEpisodeSelected: (PlayableItem) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Danh sách tập phim",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                items(playableItems) { item ->
                    val isCurrent = item.id == currentPlayableId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEpisodeSelected(item) }
                            .background(
                                if (isCurrent) PrimaryCoral.copy(alpha = 0.15f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.label,
                            color = if (isCurrent) PrimaryCoral else TextPrimaryDark,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                        if (isCurrent) {
                            Text(
                                text = "Đang phát",
                                color = PrimaryCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper function to convert seconds to MM:SS or HH:MM:SS
private fun formatTime(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

// Helper function to find Activity from Context
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
