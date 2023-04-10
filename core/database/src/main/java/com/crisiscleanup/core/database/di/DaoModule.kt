package com.crisiscleanup.core.database.di

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun providesIncidentDao(db: CrisisCleanupDatabase) = db.incidentDao()

    @Provides
    fun providesLocationDao(db: CrisisCleanupDatabase) = db.locationDao()

    @Provides
    fun providesWorksiteSyncStatsDao(db: CrisisCleanupDatabase) = db.worksitesSyncStatsDao()

    @Provides
    fun providesWorksiteDao(db: CrisisCleanupDatabase) = db.worksiteDao()

    @Provides
    fun providesWorkTypeDao(db: CrisisCleanupDatabase) = db.workTypeDao()

    @Provides
    fun providesWorksiteFormDataDao(db: CrisisCleanupDatabase) = db.worksiteFormDataDao()

    @Provides
    fun providesWorksiteFlagDao(db: CrisisCleanupDatabase) = db.worksiteFlagDao()

    @Provides
    fun providesWorksiteNoteDao(db: CrisisCleanupDatabase) = db.worksiteNoteDao()

    @Provides
    fun providesLanguageDao(db: CrisisCleanupDatabase) = db.languageDao()

    @Provides
    fun providesSyncLogDao(db: CrisisCleanupDatabase) = db.syncLogDao()

    @Provides
    fun providesWorksiteChangeDao(db: CrisisCleanupDatabase) = db.worksiteChangeDao()
}