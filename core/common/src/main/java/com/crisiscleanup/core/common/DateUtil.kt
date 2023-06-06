package com.crisiscleanup.core.common

import com.github.marlonlom.utilities.timeago.TimeAgo
import kotlinx.datetime.Instant

val Instant.relativeTime: String
    get() = TimeAgo.using(toEpochMilliseconds())