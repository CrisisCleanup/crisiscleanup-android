package com.crisiscleanup.core.common

interface Syncer {
    fun sync(force: Boolean = false, cancelOngoing: Boolean = false)

    fun syncIncident(id: Long, force: Boolean = false)
}