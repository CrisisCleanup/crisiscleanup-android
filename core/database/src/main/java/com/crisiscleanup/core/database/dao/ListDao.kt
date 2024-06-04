package com.crisiscleanup.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.ListEntity
import com.crisiscleanup.core.database.model.PopulatedList
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface ListDao {
    @Transaction
    @Query("SELECT * FROM lists WHERE incident_id=:incidentId")
    fun streamIncidentLists(incidentId: Long): Flow<List<PopulatedList>>

    @Transaction
    @Query("SELECT * FROM lists WHERE id=:id")
    fun streamList(id: Long): Flow<PopulatedList?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM lists
        ORDER BY updated_at DESC
        """,
    )
    fun pageLists(): PagingSource<Int, PopulatedList>

    @Transaction
    @Query("DELETE FROM lists WHERE network_id IN(:networkIds)")
    fun deleteLists(networkIds: Set<Long>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreList(list: ListEntity): Long

    @Transaction
    @Query(
        """
        UPDATE lists SET
        updated_by  = :updatedBy,
        updated_at  = :updatedAt,
        parent      = :parent,
        name        = :name,
        description = :description,
        list_order  = :listOrder,
        tags        = :tags,
        model       = :model,
        object_ids  = :objectIds,
        shared      = :shared,
        permissions = :permissions,
        incident_id = :incident
        WHERE network_id=:networkId AND local_global_uuid=""
        """,
    )
    fun syncUpdateList(
        networkId: Long,
        updatedBy: Long?,
        updatedAt: Instant,
        parent: Long?,
        name: String,
        description: String,
        listOrder: Long?,
        tags: String,
        model: String,
        objectIds: String,
        shared: String,
        permissions: String,
        incident: Long?,
    )
}
