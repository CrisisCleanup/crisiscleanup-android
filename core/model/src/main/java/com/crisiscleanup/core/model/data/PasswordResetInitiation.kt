package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class PasswordResetInitiation(
    val expiresAt: Instant?,
    val errorMessage: String = "",
)
