package com.crisiscleanup.core.testing.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

val nowTruncateMillis: Instant
    get() {
        return Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
    }
