package com.crisiscleanup.core.database

import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

/**
 * Automatic schema migrations sometimes require extra instructions to perform the migration, for
 * example, when a column is renamed. These extra instructions are placed here by creating a class
 * using the following naming convention `SchemaXtoY` where X is the schema version you're migrating
 * from and Y is the schema version you're migrating to. The class should implement
 * `AutoMigrationSpec`.
 */
object DatabaseMigrations {
    @DeleteTable.Entries(
        DeleteTable(
            tableName = "worksite_to_work_type",
        ),
        DeleteTable(
            tableName = "worksite_work_types",
        ),
    )
    class Schema2To3 : AutoMigrationSpec

    @RenameColumn(
        tableName = "worksite_sync_stats",
        fromColumnName = "incidentId",
        toColumnName = "incident_id",
    )
    class Schema3to4 : AutoMigrationSpec
}