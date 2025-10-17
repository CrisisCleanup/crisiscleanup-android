package com.crisiscleanup.core.common

import com.github.marlonlom.utilities.timeago.TimeAgo
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

val Instant.relativeTime: String
    get() = TimeAgo.using(toEpochMilliseconds())

val Instant.noonTime: Instant
    get() {
        val javaDate = toJavaInstant()
        val startOfDay = javaDate.truncatedTo(ChronoUnit.DAYS)
        return startOfDay.toKotlinInstant().plus(12.hours)
    }

val Instant.isPast: Boolean
    get() = this < Clock.System.now()

val DateTimeFormatter.utcTimeZone: DateTimeFormatter
    get() {
        return withZone(ZoneId.of("UTC"))
    }
