package com.example.movieapp.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format duration in seconds into human-readable string.
 * Example: 7080s -> "1h 58m", 2700s -> "45m", 3600s -> "1h", 0s -> "0m"
 */
fun Long.formatDurationSeconds(): String {
    if (this <= 0) return "0m"
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}

fun Int.formatDurationSeconds(): String = this.toLong().formatDurationSeconds()

/**
 * Format ISO 8601 date-time string (e.g. "2026-09-14T11:12:45Z" or "2026-09-14") to Vietnamese display format.
 * Output pattern: "dd/MM/yyyy" or "dd/MM/yyyy HH:mm"
 */
fun String.formatIsoToVietnameseDate(
    includeTime: Boolean = false,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (this.isBlank()) return ""
    return try {
        val pattern = if (includeTime) "dd/MM/yyyy HH:mm" else "dd/MM/yyyy"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale("vi", "VN"))

        when {
            contains("T") -> {
                try {
                    val offsetDateTime = OffsetDateTime.parse(this)
                    offsetDateTime.atZoneSameInstant(zoneId).format(formatter)
                } catch (_: Exception) {
                    val instant = Instant.parse(this)
                    instant.atZone(zoneId).format(formatter)
                }
            }
            else -> {
                val localDate = LocalDate.parse(this)
                if (includeTime) {
                    localDate.atStartOfDay(zoneId).format(formatter)
                } else {
                    localDate.format(formatter)
                }
            }
        }
    } catch (_: Exception) {
        this
    }
}
