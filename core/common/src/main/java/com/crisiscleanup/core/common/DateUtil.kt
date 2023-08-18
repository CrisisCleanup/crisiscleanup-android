package com.crisiscleanup.core.common

import com.github.marlonlom.utilities.timeago.TimeAgo
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
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
