package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class PasswordResetInitiation(
    val expiresAt: Instant?,
    val errorMessage: String = "",
)
