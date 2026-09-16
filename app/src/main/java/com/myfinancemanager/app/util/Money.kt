package com.myfinancemanager.app.util

import java.security.MessageDigest
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

object Money {
    fun format(amount: Double, currencyCode: String = "INR"): String {
        val locale = if (currencyCode == "INR") Locale("en", "IN") else Locale.US
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(currencyCode)
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }

    fun percent(gain: Double, invested: Double): Double {
        if (invested == 0.0) return 0.0
        return (gain / invested) * 100.0
    }
}

object Ids {
    fun new(): String = UUID.randomUUID().toString()

    fun fingerprint(parts: List<String>): String {
        val raw = parts.joinToString("|") { it.trim().lowercase() }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

object Dates {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val shortFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")
    private val dateTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

    fun now(): Long = System.currentTimeMillis()

    fun startOfMonth(yearMonth: YearMonth = YearMonth.now()): Long {
        return yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfMonth(yearMonth: YearMonth = YearMonth.now()): Long {
        return yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
    }

    fun toEpoch(date: LocalDate): Long {
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun toLocalDate(epoch: Long): LocalDate {
        return Instant.ofEpochMilli(epoch).atZone(zone).toLocalDate()
    }

    fun format(epoch: Long): String = toLocalDate(epoch).format(dayFmt)
    fun formatShort(epoch: Long): String = toLocalDate(epoch).format(shortFmt)
    fun formatMonth(yearMonth: YearMonth): String = yearMonth.format(monthFmt)

    /** Date and wall-clock time, used for "last synced" readouts. */
    fun formatDateTime(epoch: Long): String =
        Instant.ofEpochMilli(epoch).atZone(zone).format(dateTimeFmt)

    fun monthsBack(count: Int): List<YearMonth> {
        val now = YearMonth.now()
        return (count - 1 downTo 0).map { now.minusMonths(it.toLong()) }
    }

    fun almostSameDay(a: Long, b: Long): Boolean {
        return abs(toLocalDate(a).toEpochDay() - toLocalDate(b).toEpochDay()) <= 1
    }
}

fun String.titleCase(): String {
    return lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }
}
