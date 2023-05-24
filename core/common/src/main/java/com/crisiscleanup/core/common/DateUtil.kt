package com.crisiscleanup.core.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private val absoluteDateFormat =
    DateTimeFormatter.ofPattern("MMM dd yyyy").withZone(ZoneId.systemDefault())

private fun pluralize(amount: Long, singular: String): String {
    val plural = if (amount > 1) "s" else ""
    return "$amount $singular${plural} ago"
}

fun Instant.relativeTime(reference: Instant = Clock.System.now()): String {
    // TODO Localize

    val delta = reference - this
    if (delta < 1.minutes) {
        return ""
    }

    if (delta < 1.hours) {
        return pluralize(delta.inWholeMinutes, "minute")
    }

    if (delta < 1.days) {
        return pluralize(delta.inWholeHours, "hour")
    }

    if (delta < 30.days) {
        return pluralize(delta.inWholeDays, "hour")
    }

    if (delta < 180.days) {
        return pluralize(delta.inWholeDays / 30, "month")
    }

    return absoluteDateFormat.format(toJavaInstant())
}