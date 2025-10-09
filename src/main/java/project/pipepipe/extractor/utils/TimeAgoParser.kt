package project.pipepipe.extractor.utils

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

object TimeAgoParser {

    private val TIME_PATTERNS = mapOf(
        ChronoUnit.SECONDS to listOf("second", "seconds", "sec", "secs"),
        ChronoUnit.MINUTES to listOf("minute", "minutes", "min", "mins"),
        ChronoUnit.HOURS to listOf("hour", "hours", "hr", "hrs"),
        ChronoUnit.DAYS to listOf("day", "days"),
        ChronoUnit.WEEKS to listOf("week", "weeks"),
        ChronoUnit.MONTHS to listOf("month", "months"),
        ChronoUnit.YEARS to listOf("year", "years")
    )

    private val SPECIAL_CASES = mapOf(
        "now" to (ChronoUnit.SECONDS to 0),
        "just now" to (ChronoUnit.SECONDS to 0),
        "a moment ago" to (ChronoUnit.SECONDS to 30),
        "yesterday" to (ChronoUnit.DAYS to 1),
        "a second ago" to (ChronoUnit.SECONDS to 1),
        "a minute ago" to (ChronoUnit.MINUTES to 1),
        "an hour ago" to (ChronoUnit.HOURS to 1),
        "a day ago" to (ChronoUnit.DAYS to 1)
    )

    /**
     * Parses "X time ago" format and returns epoch timestamp in milliseconds
     */
    fun parseToTimestamp(textualDate: String): Long {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Check special cases first
        SPECIAL_CASES[textualDate.lowercase()]?.let { (unit, amount) ->
            return now.minus(amount.toLong(), unit).toInstant().toEpochMilli()
        }

        val amount = extractAmount(textualDate)
        val unit = extractTimeUnit(textualDate)

        val resultDateTime = when (unit) {
            ChronoUnit.YEARS -> now.minusYears(amount.toLong()).minusDays(1)
            else -> now.minus(amount.toLong(), unit)
        }

        return resultDateTime.toInstant().toEpochMilli()
    }

    /**
     * Parses duration text and returns duration in seconds
     */
    fun parseDuration(textualDuration: String): Long {
        val amount = extractAmount(textualDuration)
        val unit = extractTimeUnit(textualDuration)

        return amount * unit.duration.seconds
    }

    private fun extractAmount(text: String): Int {
        return try {
            text.replace(Regex("\\D+"), "").toInt()
        } catch (e: NumberFormatException) {
            1 // Default to 1 if no number found (as in "a second ago")
        }
    }

    private fun extractTimeUnit(text: String): ChronoUnit {
        val lowerText = text.lowercase()

        return TIME_PATTERNS.entries.firstOrNull { (_, patterns) ->
            patterns.any { pattern ->
                textContainsPattern(lowerText, pattern)
            }
        }?.key ?: ChronoUnit.SECONDS // Default to seconds if no match
    }

    private fun textContainsPattern(text: String, pattern: String): Boolean {
        // Simple contains check for exact matches
        if (text == pattern) return true

        // Word boundary check for partial matches
        val escapedPattern = Pattern.quote(pattern)
        val regex = "(^|\\s)$escapedPattern(\\s|$)".toRegex()
        return regex.containsMatchIn(text)
    }
}
