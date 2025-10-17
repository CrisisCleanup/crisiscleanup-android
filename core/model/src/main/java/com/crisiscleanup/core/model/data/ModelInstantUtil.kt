package com.crisiscleanup.core.model.data

import kotlin.time.Clock
import kotlin.time.Instant

val Instant.isPast: Boolean
    get() = this < Clock.System.now()
