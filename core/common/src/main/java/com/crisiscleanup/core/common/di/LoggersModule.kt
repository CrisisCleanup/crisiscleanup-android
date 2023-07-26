package com.crisiscleanup.core.common.di

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.log.TagLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LoggersModule {
    @Provides
    @Logger(CrisisCleanupLoggers.App)
    fun providesAppLogger(logger: TagLogger): AppLogger {
        logger.tag = "app"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Auth)
    fun providesAuthLogger(logger: TagLogger): AppLogger {
        logger.tag = "auth"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Cases)
    fun providesCasesLogger(logger: TagLogger): AppLogger {
        logger.tag = "cases"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Navigation)
    fun providesNavLogger(logger: TagLogger): AppLogger {
        logger.tag = "navigation"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Network)
    fun providesNetworkLogger(logger: TagLogger): AppLogger {
        logger.tag = "network"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Token)
    fun providesTokenLogger(logger: TagLogger): AppLogger {
        logger.tag = "token"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Incidents)
    fun providesIncidentsLogger(logger: TagLogger): AppLogger {
        logger.tag = "incidents"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Worksites)
    fun providesWorksitesLogger(logger: TagLogger): AppLogger {
        logger.tag = "worksites"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Language)
    fun providesLanguageLogger(logger: TagLogger): AppLogger {
        logger.tag = "language"
        return logger
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Media)
    fun providesMediaLogger(logger: TagLogger): AppLogger {
        logger.tag = "media"
        return logger
    }
}
