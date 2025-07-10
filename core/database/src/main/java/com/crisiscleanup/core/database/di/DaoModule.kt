package com.crisiscleanup.core.database.di

import com.crisiscleanup.core.database.CrisisCleanupDatabase
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
    fun providesWorksiteDao(db: CrisisCleanupDatabase) = db.worksiteDao()

    @Provides
    fun providesWorkTypeDao(db: CrisisCleanupDatabase) = db.workTypeDao()

    @Provides
    fun providesWorkTypeStatusDao(db: CrisisCleanupDatabase) = db.workTypeStatusDao()

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

    @Provides
    fun providesIncidentOrganizationDao(db: CrisisCleanupDatabase) = db.incidentOrganizationDao()

    @Provides
    fun providesPersonContactDao(db: CrisisCleanupDatabase) = db.personContactDao()

    @Provides
    fun recentWorksiteDao(db: CrisisCleanupDatabase) = db.recentWorksiteDao()

    @Provides
    fun workTypeTransferRequestDao(db: CrisisCleanupDatabase) = db.workTypeTransferRequestDao()

    @Provides
    fun networkFileDao(db: CrisisCleanupDatabase) = db.networkFileDao()

    @Provides
    fun localImageDao(db: CrisisCleanupDatabase) = db.localImageDao()

    @Provides
    fun caseHistoryDao(db: CrisisCleanupDatabase) = db.caseHistoryDao()

    @Provides
    fun listDao(db: CrisisCleanupDatabase) = db.listDao()

    @Provides
    fun incidentDataSyncParametersDao(db: CrisisCleanupDatabase) =
        db.incidentDataSyncParametersDao()

    @Provides
    fun teamDao(db: CrisisCleanupDatabase) = db.teamDao()

    @Provides
    fun equipmentDao(db: CrisisCleanupDatabase) = db.equipmentDao()

    @Provides
    fun userRoleDao(db: CrisisCleanupDatabase) = db.userRoleDao()
}
