package com.crisiscleanup.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.database.DatabaseMigrations.Schema10To11
import com.crisiscleanup.core.database.DatabaseMigrations.Schema2To3
import com.crisiscleanup.core.database.DatabaseMigrations.Schema3to4
import com.crisiscleanup.core.database.dao.*
import com.crisiscleanup.core.database.model.*
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
        IncidentOrganizationSyncStatsEntity::class,
        RecentWorksiteEntity::class,
    ],
    version = 16,
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
}
