package com.crisiscleanup.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crisiscleanup.core.database.DatabaseMigrations.Schema2To3
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.LocationDao
import com.crisiscleanup.core.database.dao.TestTargetIncidentDao
import com.crisiscleanup.core.database.dao.TestTargetWorkTypeDao
import com.crisiscleanup.core.database.dao.TestTargetWorksiteDao
import com.crisiscleanup.core.database.dao.WorkTypeDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.LocationEntity
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.WorksiteSyncStatsEntity
import com.crisiscleanup.core.database.util.InstantConverter

@Database(
    entities = [
        IncidentEntity::class,
        IncidentLocationEntity::class,
        IncidentIncidentLocationCrossRef::class,
        LocationEntity::class,
        WorksiteSyncStatsEntity::class,
        WorksiteRootEntity::class,
        WorksiteEntity::class,
        WorkTypeEntity::class,
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = Schema2To3::class),
    ],
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
)
abstract class CrisisCleanupDatabase : RoomDatabase() {
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
