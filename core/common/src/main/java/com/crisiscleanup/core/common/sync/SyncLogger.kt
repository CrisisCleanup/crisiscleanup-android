package com.crisiscleanup.core.common.sync

interface SyncLogger {
    fun log(message: String, type: String = "", details: String = ""): SyncLogger
    fun flush()
}
