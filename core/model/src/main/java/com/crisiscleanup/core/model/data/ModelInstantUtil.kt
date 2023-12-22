package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

val Instant.isPast: Boolean
    get() = this < Clock.System.now()
