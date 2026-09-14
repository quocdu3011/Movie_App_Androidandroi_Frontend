package com.example.movieapp.core.ui.component

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.movieapp.core.ui.theme.MovieAppTheme

/**
 * Format resume position in seconds to "mm:ss" or "hh:mm:ss"
 */
private fun formatTimePosition(seconds: Int): String {
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

@Composable
fun ResumeConfirmDialog(
    resumePositionSeconds: Int,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val formattedTime = formatTimePosition(resumePositionSeconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tiếp tục xem phim?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "Bạn có muốn tiếp tục xem từ vị trí $formattedTime không?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(true) }
            ) {
                Text(
                    text = "Tiếp tục",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm(false) }
            ) {
                Text(
                    text = "Xem lại từ đầu",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        iconContentColor = MaterialTheme.colorScheme.primary
    )
}

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ResumeConfirmDialogLightPreview() {
    MovieAppTheme(darkTheme = false) {
        ResumeConfirmDialog(
            resumePositionSeconds = 708, // 11:48
            onConfirm = {}
        )
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResumeConfirmDialogDarkPreview() {
    MovieAppTheme(darkTheme = true) {
        ResumeConfirmDialog(
            resumePositionSeconds = 708, // 11:48
            onConfirm = {}
        )
    }
}
