package com.example.movieapp.core.common

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `test formatDurationSeconds formats seconds correctly`() {
        assertEquals("1h 58m", 7080L.formatDurationSeconds())
        assertEquals("45m", 2700L.formatDurationSeconds())
        assertEquals("1h", 3600L.formatDurationSeconds())
        assertEquals("0m", 0L.formatDurationSeconds())
        assertEquals("0m", (-50L).formatDurationSeconds())
        assertEquals("1h 58m", 7080.formatDurationSeconds())
    }

    @Test
    fun `test formatIsoToVietnameseDate with UTC zone`() {
        val utcZone = ZoneId.of("UTC")
        val isoDateTime = "2026-09-14T11:12:45Z"
        val isoDateOnly = "2026-09-14"

        assertEquals("14/09/2026", isoDateTime.formatIsoToVietnameseDate(includeTime = false, zoneId = utcZone))
        assertEquals("14/09/2026 11:12", isoDateTime.formatIsoToVietnameseDate(includeTime = true, zoneId = utcZone))
        assertEquals("14/09/2026", isoDateOnly.formatIsoToVietnameseDate(includeTime = false, zoneId = utcZone))
        assertEquals("14/09/2026 00:00", isoDateOnly.formatIsoToVietnameseDate(includeTime = true, zoneId = utcZone))
    }

    @Test
    fun `test formatIsoToVietnameseDate handles malformed strings safely`() {
        val invalidIso = "invalid-date-string"
        assertEquals("invalid-date-string", invalidIso.formatIsoToVietnameseDate())
    }
}
