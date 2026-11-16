package ca.nexacraft.homekeep.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Dates {

    private val pretty = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun parse(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
    }

    fun pretty(value: String?, fallback: String = "Not set"): String =
        parse(value)?.format(pretty) ?: fallback

    fun today(): String = LocalDate.now().toString()

    fun daysUntil(value: String?): Long? =
        parse(value)?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    fun addMonths(value: String?, months: Int): String? =
        parse(value)?.plusMonths(months.toLong())?.toString()

    fun fromUtcMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

    fun toUtcMillis(value: String?): Long? =
        parse(value)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
}
