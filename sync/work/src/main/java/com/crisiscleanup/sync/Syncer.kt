package com.crisiscleanup.sync

interface Syncer {
    suspend fun sync(force: Boolean = false)
}