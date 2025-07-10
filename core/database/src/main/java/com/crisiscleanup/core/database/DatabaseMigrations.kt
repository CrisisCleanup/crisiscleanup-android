package com.crisiscleanup.core.database

import androidx.room.DeleteColumn
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

    @DeleteColumn.Entries(
        DeleteColumn(
            tableName = "worksite_flags",
            columnName = "is_invalid",
        ),
        DeleteColumn(
            tableName = "work_types",
            columnName = "is_invalid",
        ),
    )
    class Schema10To11 : AutoMigrationSpec

    @DeleteColumn.Entries(
        DeleteColumn(
            tableName = "worksite_flags",
            columnName = "local_global_uuid",
        ),
        DeleteColumn(
            tableName = "work_types",
            columnName = "local_global_uuid",
        ),
    )
    class Schema18To19 : AutoMigrationSpec

    @DeleteTable.Entries(
        DeleteTable(
            tableName = "worksite_text_fts",
        ),
    )
    class Schema35To36 : AutoMigrationSpec

    @DeleteTable.Entries(
        DeleteTable(
            tableName = "worksite_text_fts_b",
        ),
    )
    class Schema45To46 : AutoMigrationSpec
}
