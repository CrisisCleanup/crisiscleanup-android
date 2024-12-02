package com.crisiscleanup.core.common

import com.github.marlonlom.utilities.timeago.TimeAgo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours

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

val Instant.Companion.epochZero: Instant
    get() = fromEpochSeconds(0)
