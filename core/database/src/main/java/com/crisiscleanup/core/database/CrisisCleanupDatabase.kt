package com.crisiscleanup.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crisiscleanup.core.database.dao.IncidentDao
import com.crisiscleanup.core.database.dao.IncidentLocationDao
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.util.InstantConverter

@Database(
    entities = [
        IncidentEntity::class,
        IncidentLocationEntity::class,
        IncidentIncidentLocationCrossRef::class,
    ],
    version = 1,
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
    abstract fun incidentLocationDao(): IncidentLocationDao
}
