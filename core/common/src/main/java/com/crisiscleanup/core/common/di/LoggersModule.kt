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
    private fun TagLogger.applyTag(tag: String): AppLogger {
        this.tag = tag
        return this
    }

    @Provides
    @Logger(CrisisCleanupLoggers.Account)
    fun providesAccountLogger(logger: TagLogger) = logger.applyTag("account")

    @Provides
    @Logger(CrisisCleanupLoggers.App)
    fun providesAppLogger(logger: TagLogger) = logger.applyTag("app")

    @Provides
    @Logger(CrisisCleanupLoggers.Auth)
    fun providesAuthLogger(logger: TagLogger) = logger.applyTag("auth")

    @Provides
    @Logger(CrisisCleanupLoggers.Cases)
    fun providesCasesLogger(logger: TagLogger) = logger.applyTag("cases")

    @Provides
    @Logger(CrisisCleanupLoggers.Incidents)
    fun providesIncidentsLogger(logger: TagLogger) = logger.applyTag("incidents")

    @Provides
    @Logger(CrisisCleanupLoggers.Language)
    fun providesLanguageLogger(logger: TagLogger) = logger.applyTag("language")

    @Provides
    @Logger(CrisisCleanupLoggers.Lists)
    fun providesListsLogger(logger: TagLogger) = logger.applyTag("lists")

    @Provides
    @Logger(CrisisCleanupLoggers.Media)
    fun providesMediaLogger(logger: TagLogger) = logger.applyTag("media")

    @Provides
    @Logger(CrisisCleanupLoggers.Navigation)
    fun providesNavLogger(logger: TagLogger) = logger.applyTag("navigation")

    @Provides
    @Logger(CrisisCleanupLoggers.Network)
    fun providesNetworkLogger(logger: TagLogger) = logger.applyTag("network")

    @Provides
    @Logger(CrisisCleanupLoggers.Onboarding)
    fun providesOnboardingLogger(logger: TagLogger) = logger.applyTag("onboarding")

    @Provides
    @Logger(CrisisCleanupLoggers.Sync)
    fun providesSyncLogger(logger: TagLogger) = logger.applyTag("sync")

    @Provides
    @Logger(CrisisCleanupLoggers.Team)
    fun providesTeamLogger(logger: TagLogger) = logger.applyTag("team")

    @Provides
    @Logger(CrisisCleanupLoggers.Token)
    fun providesTokenLogger(logger: TagLogger) = logger.applyTag("token")

    @Provides
    @Logger(CrisisCleanupLoggers.Worksites)
    fun providesWorksitesLogger(logger: TagLogger) = logger.applyTag("worksites")
}
