package com.crisiscleanup.core.common.sync

interface SyncLogger {
    var type: String
    fun log(message: String, details: String = "", type: String = ""): SyncLogger
    fun clear(): SyncLogger
    fun flush()
}
