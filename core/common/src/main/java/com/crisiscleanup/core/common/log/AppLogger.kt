package com.crisiscleanup.core.common.log

interface AppLogger {
    var tag: String?

    fun logDebug(vararg logs: Any)

    fun logException(e: Exception)
}