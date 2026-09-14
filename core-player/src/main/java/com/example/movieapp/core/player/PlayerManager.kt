package com.example.movieapp.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.example.movieapp.core.network.di.MediaClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlayerManager wraps Media3 ExoPlayer for HLS and media playback.
 *
 * IMPORTANT SECURITY REQUIREMENT:
 * PlayerManager MUST receive a qualified `@MediaClient` OkHttpClient injected from outside.
 * This client has NO AuthInterceptor or TokenAuthenticator attached, ensuring that:
 * 1. Bearer JWT tokens are NEVER leaked to third-party CDN or media servers.
 * 2. Media 401 Unauthorized responses do NOT trigger refresh token rotation API calls.
 * 3. It shares the exact same CookieJar instance with BackendApiClient to handle HttpOnly session cookies for owned HLS streams.
 */
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:MediaClient val mediaHttpClient: OkHttpClient
) {
    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            updatePlayerState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayerState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.value = PlayerState.Error(error)
        }
    }

    @OptIn(UnstableApi::class)
    fun prepare(playbackUrl: String, startPositionMs: Long = 0L) {
        val player = getOrCreatePlayer()

        val dataSourceFactory = OkHttpDataSource.Factory(mediaHttpClient)
        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(playbackUrl))

        player.setMediaSource(mediaSource)
        player.prepare()
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
    }

    fun getPlaybackSpeed(): Float {
        return exoPlayer?.playbackParameters?.speed ?: 1.0f
    }

    fun release() {
        exoPlayer?.removeListener(listener)
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState.Idle
    }

    fun currentPositionSeconds(): Int {
        val currentMs = exoPlayer?.currentPosition ?: 0L
        return (currentMs / 1000).toInt()
    }

    fun durationSeconds(): Int? {
        val durationMs = exoPlayer?.duration ?: return null
        if (durationMs <= 0 || durationMs == C.TIME_UNSET) return null
        return (durationMs / 1000).toInt()
    }

    fun getPlayer(): Player? = exoPlayer

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            newPlayer.addListener(listener)
            exoPlayer = newPlayer
        }
    }

    private fun updatePlayerState() {
        val player = exoPlayer ?: run {
            _playerState.value = PlayerState.Idle
            return
        }

        if (player.playerError != null) {
            _playerState.value = PlayerState.Error(player.playerError)
            return
        }

        _playerState.value = when (player.playbackState) {
            Player.STATE_IDLE -> PlayerState.Idle
            Player.STATE_BUFFERING -> PlayerState.Buffering
            Player.STATE_ENDED -> PlayerState.Ended
            Player.STATE_READY -> if (player.isPlaying) PlayerState.Playing else PlayerState.Paused
            else -> PlayerState.Idle
        }
    }
}
