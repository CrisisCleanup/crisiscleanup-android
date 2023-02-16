package com.crisiscleanup.core.common

interface Syncer {
    fun sync(force: Boolean = false)
}