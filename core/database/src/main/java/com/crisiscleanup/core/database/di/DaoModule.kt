package com.crisiscleanup.core.database.di

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.dao.IncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    @Provides
    fun providesIncidentDao(
        database: CrisisCleanupDatabase,
    ): IncidentDao = database.incidentDao()
}