package id.ac.usm.sipandutambak.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object WibTimeUtil {
    val WIB_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    private val ID_LOCALE = Locale("id", "ID")

    /**
     * Parses Supabase ISO-8601 timestamps (UTC `+00:00`, `Z`, with/without microseconds,
     * or naive timestamp interpreted as UTC) into an [Instant].
     */
    fun parseIsoToInstant(rawDate: String?): Instant? {
        val cleaned = rawDate?.trim().orEmpty()
        if (cleaned.isEmpty() || cleaned.equals("null", ignoreCase = true)) return null

        val normalized = buildString {
            var s = cleaned.replace(' ', 'T')
            if (Regex(".*[+-]\\d{2}$").matches(s)) {
                s += ":00"
            }
            append(s)
        }

        return try {
            Instant.parse(normalized)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(normalized).toInstant()
            } catch (_: Exception) {
                try {
                    val base = if (normalized.length >= 19) normalized.substring(0, 19) else normalized
                    LocalDateTime.parse(base).toInstant(ZoneOffset.UTC)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    /** Formats an ISO-8601 string into Asia/Jakarta (WIB / UTC+7). */
    fun formatIsoToWibTime(rawDate: String?, pattern: String = "HH:mm:ss"): String {
        val instant = parseIsoToInstant(rawDate) ?: return "-"
        val wibTime = ZonedDateTime.ofInstant(instant, WIB_ZONE)
        return DateTimeFormatter.ofPattern(pattern, ID_LOCALE).format(wibTime)
    }

    /** Formats current device time forced to Asia/Jakarta (WIB / UTC+7). */
    fun formatNowWib(pattern: String = "EEEE, dd MMMM yyyy"): String {
        val nowWib = ZonedDateTime.now(WIB_ZONE)
        return DateTimeFormatter.ofPattern(pattern, ID_LOCALE).format(nowWib)
    }

    /** Returns true when timestamp is within [maxMinutes] relative to current UTC/WIB instant. */
    fun isWithinMinutes(rawDate: String?, maxMinutes: Long = 5L): Boolean {
        val instant = parseIsoToInstant(rawDate) ?: return false
        val diffMinutes = Duration.between(instant, Instant.now()).toMinutes()
        return diffMinutes in -2L..maxMinutes
    }

    /** Evaluates device online state from latest log `created_at` or device `last_seen`. */
    fun isDeviceOnline(createdAt: String?, lastSeen: String?, fallbackFlag: Boolean = false): Boolean {
        val hasCreatedAt = parseIsoToInstant(createdAt) != null
        val hasLastSeen = parseIsoToInstant(lastSeen) != null
        return when {
            hasCreatedAt -> isWithinMinutes(createdAt, maxMinutes = 5L)
            hasLastSeen -> isWithinMinutes(lastSeen, maxMinutes = 5L)
            else -> fallbackFlag
        }
    }
}
