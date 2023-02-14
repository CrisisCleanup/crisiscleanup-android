package com.crisiscleanup.core.common

interface Syncer {
    suspend fun sync(force: Boolean = false)
}