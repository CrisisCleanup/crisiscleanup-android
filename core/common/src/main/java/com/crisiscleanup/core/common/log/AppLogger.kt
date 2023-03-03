package com.crisiscleanup.core.common.log

import javax.inject.Qualifier

interface AppLogger {
    fun logDebug(vararg logs: Any)

    fun logException(e: Exception)
}

interface TagLogger : AppLogger {
    var tag: String?
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Logger(val loggers: CrisisCleanupLoggers)

enum class CrisisCleanupLoggers {
    Auth,
    Cases,
    Navigation,
    Token,
    Incidents,
    Worksites,
    Language,
}
