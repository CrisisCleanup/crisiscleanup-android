package com.crisiscleanup.core.database.di

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.dao.LocationDao
import com.crisiscleanup.core.database.dao.LocationDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun providesIncidentDao(
        db: CrisisCleanupDatabase,
    ): IncidentDao = db.incidentDao()

    @Provides
    fun providesIncidentDaoPlus(
        db: CrisisCleanupDatabase,
    ) = IncidentDaoPlus(db)

    @Provides
    fun providesLocationDao(
        db: CrisisCleanupDatabase,
    ): LocationDao = db.locationDao()

    @Provides
    fun providesLocationDaoPlus(
        db: CrisisCleanupDatabase,
    ) = LocationDaoPlus(db)

    @Provides
    fun providesWorksiteSyncStatsDao(
        db: CrisisCleanupDatabase,
    ): WorksitesSyncStatsDao = db.worksitesSyncStatsDao()

    @Provides
    fun providesWorksiteDao(
        db: CrisisCleanupDatabase,
    ): WorksiteDao = db.worksiteDao()

    @Provides
    fun providesWorksiteDaoPlus(
        db: CrisisCleanupDatabase,
    ) = WorksiteDaoPlus(db)
}