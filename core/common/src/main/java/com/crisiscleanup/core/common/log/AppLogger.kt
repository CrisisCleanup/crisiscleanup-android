package com.crisiscleanup.core.common.log

import javax.inject.Qualifier

interface AppLogger {
    fun logDebug(vararg logs: Any)

    fun logException(e: Exception)

    fun logCapture(message: String)
}

interface TagLogger : AppLogger {
    var tag: String?
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Logger(val loggers: CrisisCleanupLoggers)

enum class CrisisCleanupLoggers {
    Account,
    App,
    Auth,
    Cases,
    Incidents,
    Language,
    Media,
    Navigation,
    Network,
    Onboarding,
    Sync,
    Token,
    Worksites,
}
