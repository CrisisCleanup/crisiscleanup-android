package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import com.crisiscleanup.core.database.model.WorksiteChangeEntity

@Dao
interface WorksiteChangeDao {
    @Insert
    fun insertChange(change: WorksiteChangeEntity)
}