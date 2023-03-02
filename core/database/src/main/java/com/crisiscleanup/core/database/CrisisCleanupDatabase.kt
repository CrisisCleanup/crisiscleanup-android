package com.crisiscleanup.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crisiscleanup.core.common.DatabaseVersionProvider
import com.crisiscleanup.core.database.DatabaseMigrations.Schema2To3
import com.crisiscleanup.core.database.DatabaseMigrations.Schema3to4
import com.crisiscleanup.core.database.dao.*
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.database.util.InstantConverter

@Database(
    entities = [
        IncidentEntity::class,
        IncidentLocationEntity::class,
        IncidentIncidentLocationCrossRef::class,
        IncidentFormFieldEntity::class,
        LocationEntity::class,
        WorksiteSyncStatsEntity::class,
        WorksiteRootEntity::class,
        WorksiteEntity::class,
        WorkTypeEntity::class,
    ],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = Schema2To3::class),
        AutoMigration(from = 3, to = 4, spec = Schema3to4::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
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
    abstract fun worksitesSyncStatsDao(): WorksitesSyncStatsDao
    abstract fun worksiteDao(): WorksiteDao
    abstract fun workTypeDao(): WorkTypeDao

    // TODO Restrict below to test builds only
    abstract fun testTargetIncidentDao(): TestTargetIncidentDao
    abstract fun testTargetWorksiteDao(): TestTargetWorksiteDao
    abstract fun testTargetWorkTypeDao(): TestTargetWorkTypeDao
}
