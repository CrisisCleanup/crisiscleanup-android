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
    fun providesIncidentDaoPlus(db: CrisisCleanupDatabase) = IncidentDaoPlus(db)

    @Provides
    fun providesLocationDao(db: CrisisCleanupDatabase) = db.locationDao()

    @Provides
    fun providesLocationDaoPlus(db: CrisisCleanupDatabase) = LocationDaoPlus(db)

    @Provides
    fun providesWorksiteSyncStatsDao(db: CrisisCleanupDatabase) = db.worksitesSyncStatsDao()

    @Provides
    fun providesWorksiteDao(db: CrisisCleanupDatabase) = db.worksiteDao()

    @Provides
    fun providesWorksiteDaoPlus(db: CrisisCleanupDatabase) = WorksiteDaoPlus(db)

    @Provides
    fun providesWorkTypeDao(db: CrisisCleanupDatabase) = db.workTypeDao()

    @Provides
    fun providesWorkTypeDaoPlus(db: CrisisCleanupDatabase) = WorkTypeDaoPlus(db)

    @Provides
    fun providesWorksiteFormDataDao(db: CrisisCleanupDatabase) = db.worksiteFormDataDao()

    @Provides
    fun providesWorksiteFlagDao(db: CrisisCleanupDatabase) = db.worksiteFlagDao()

    @Provides
    fun providesWorksiteFlagDaoPlus(db: CrisisCleanupDatabase) = WorksiteFlagDaoPlus(db)

    @Provides
    fun providesWorksiteNoteDao(db: CrisisCleanupDatabase) = db.worksiteNoteDao()

    @Provides
    fun providesWorksiteNoteDaoPlus(db: CrisisCleanupDatabase) = WorksiteNoteDaoPlus(db)

    @Provides
    fun providesLanguageDao(db: CrisisCleanupDatabase) = db.languageDao()

    @Provides
    fun providesLanguageDaoPlus(db: CrisisCleanupDatabase) = LanguageDaoPlus(db)
}