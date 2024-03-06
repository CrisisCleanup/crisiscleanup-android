package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.PopulatedWorkTypeStatus
import com.crisiscleanup.core.database.model.WorkTypeStatusEntity

@Dao
interface WorkTypeStatusDao {
    @Upsert
    fun upsert(statuses: Collection<WorkTypeStatusEntity>)

    @Transaction
    @Query("SELECT COUNT(status) FROM work_type_statuses")
    fun getCount(): Int

    @Transaction
    @Query("SELECT status, name, primary_state FROM work_type_statuses ORDER BY list_order ASC")
    fun getStatuses(): List<PopulatedWorkTypeStatus>
}
