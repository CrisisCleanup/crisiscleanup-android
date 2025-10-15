package com.crisiscleanup.core.testing.util

import kotlin.time.Clock
import kotlin.time.Instant

val nowTruncateMillis: Instant
    get() {
        return Instant.fromEpochSeconds(Clock.System.now().epochSeconds)
    }
