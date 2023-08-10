package com.crisiscleanup.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.database.DatabaseMigrations.Schema10To11
import com.crisiscleanup.core.database.DatabaseMigrations.Schema18To19
import com.crisiscleanup.core.database.DatabaseMigrations.Schema2To3
import com.crisiscleanup.core.database.DatabaseMigrations.Schema35To36
import com.crisiscleanup.core.database.DatabaseMigrations.Schema3to4
import com.crisiscleanup.core.database.dao.CaseHistoryDao
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.LanguageDao
import com.crisiscleanup.core.database.dao.LocalImageDao
import com.crisiscleanup.core.database.dao.LocationDao
import com.crisiscleanup.core.database.dao.NetworkFileDao
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.dao.RecentWorksiteDao
import com.crisiscleanup.core.database.dao.SyncLogDao
import com.crisiscleanup.core.database.dao.WorkTypeDao
import com.crisiscleanup.core.database.dao.WorkTypeStatusDao
import com.crisiscleanup.core.database.dao.WorkTypeTransferRequestDao
import com.crisiscleanup.core.database.dao.WorksiteChangeDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteFlagDao
import com.crisiscleanup.core.database.dao.WorksiteFormDataDao
import com.crisiscleanup.core.database.dao.WorksiteNoteDao
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.dao.fts.IncidentFtsEntity
import com.crisiscleanup.core.database.dao.fts.IncidentOrganizationFtsEntity
import com.crisiscleanup.core.database.dao.fts.WorksiteTextFtsEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventAttrEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventEntity
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentFormFieldEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.database.model.IncidentWorksitesFullSyncStatsEntity
import com.crisiscleanup.core.database.model.LanguageTranslationEntity
import com.crisiscleanup.core.database.model.LocationEntity
import com.crisiscleanup.core.database.model.NetworkFileEntity
import com.crisiscleanup.core.database.model.NetworkFileLocalImageEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import com.crisiscleanup.core.database.model.SyncLogEntity
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorkTypeStatusEntity
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteLocalImageEntity
import com.crisiscleanup.core.database.model.WorksiteNetworkFileCrossRef
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.WorksiteSyncStatsEntity
import com.crisiscleanup.core.database.util.InstantConverter

@Database(
    entities = [
        WorkTypeStatusEntity::class,
        IncidentEntity::class,
        IncidentLocationEntity::class,
        IncidentIncidentLocationCrossRef::class,
        IncidentFormFieldEntity::class,
        LocationEntity::class,
        WorksiteSyncStatsEntity::class,
        WorksiteRootEntity::class,
        WorksiteEntity::class,
        WorkTypeEntity::class,
        WorksiteFormDataEntity::class,
        WorksiteFlagEntity::class,
        WorksiteNoteEntity::class,
        LanguageTranslationEntity::class,
        SyncLogEntity::class,
        WorksiteChangeEntity::class,
        IncidentOrganizationEntity::class,
        PersonContactEntity::class,
        OrganizationPrimaryContactCrossRef::class,
        OrganizationAffiliateEntity::class,
        IncidentOrganizationSyncStatsEntity::class,
        RecentWorksiteEntity::class,
        WorkTypeTransferRequestEntity::class,
        NetworkFileEntity::class,
        WorksiteNetworkFileCrossRef::class,
        NetworkFileLocalImageEntity::class,
        WorksiteLocalImageEntity::class,
        IncidentWorksitesFullSyncStatsEntity::class,
        IncidentFtsEntity::class,
        IncidentOrganizationFtsEntity::class,
        CaseHistoryEventEntity::class,
        CaseHistoryEventAttrEntity::class,
        PersonOrganizationCrossRef::class,
        WorksiteTextFtsEntity::class,
    ],
    version = 38,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = Schema2To3::class),
        AutoMigration(from = 3, to = 4, spec = Schema3to4::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = Schema10To11::class),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19, spec = Schema18To19::class),
        AutoMigration(from = 19, to = 20),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 30, to = 31),
        AutoMigration(from = 31, to = 32),
        AutoMigration(from = 32, to = 33),
        AutoMigration(from = 33, to = 34),
        AutoMigration(from = 34, to = 35),
        AutoMigration(from = 35, to = 36, spec = Schema35To36::class),
        AutoMigration(from = 36, to = 37),
        AutoMigration(from = 37, to = 38),
    ],
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
)
abstract class CrisisCleanupDatabase : RoomDatabase(), DatabaseVersionProvider {
    override val databaseVersion: Int
        get() = openHelper.readableDatabase.version

    abstract fun incidentDao(): IncidentDao
    abstract fun locationDao(): LocationDao
    abstract fun worksiteSyncStatDao(): WorksiteSyncStatDao
    abstract fun worksiteDao(): WorksiteDao
    abstract fun workTypeDao(): WorkTypeDao
    abstract fun workTypeStatusDao(): WorkTypeStatusDao
    abstract fun worksiteFormDataDao(): WorksiteFormDataDao
    abstract fun worksiteFlagDao(): WorksiteFlagDao
    abstract fun worksiteNoteDao(): WorksiteNoteDao
    abstract fun languageDao(): LanguageDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun worksiteChangeDao(): WorksiteChangeDao
    abstract fun incidentOrganizationDao(): IncidentOrganizationDao
    abstract fun personContactDao(): PersonContactDao
    abstract fun recentWorksiteDao(): RecentWorksiteDao
    abstract fun workTypeTransferRequestDao(): WorkTypeTransferRequestDao
    abstract fun networkFileDao(): NetworkFileDao
    abstract fun localImageDao(): LocalImageDao
    abstract fun caseHistoryDao(): CaseHistoryDao
}
