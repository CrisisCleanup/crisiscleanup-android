package com.crisiscleanup.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksitesSyncStatsDao
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.WorksiteSyncStatsEntity
import com.crisiscleanup.core.database.model.WorksiteWorkTypeCrossRef
import com.crisiscleanup.core.database.util.InstantConverter

@Database(
    entities = [
        IncidentEntity::class,
        IncidentLocationEntity::class,
        IncidentIncidentLocationCrossRef::class,
        WorksiteSyncStatsEntity::class,
        WorksiteRootEntity::class,
        WorksiteEntity::class,
        WorkTypeEntity::class,
        WorksiteWorkTypeCrossRef::class,
    ],
    version = 2,
    autoMigrations = [
        // See starting file and read docs for more
    ],
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
)
abstract class CrisisCleanupDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun worksitesSyncStatsDao(): WorksitesSyncStatsDao
    abstract fun worksiteDao(): WorksiteDao
}
