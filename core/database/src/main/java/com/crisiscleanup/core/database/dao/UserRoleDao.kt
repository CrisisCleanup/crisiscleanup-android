package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.UserRoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRoleDao {
    @Transaction
    @Query("SELECT * FROM user_roles")
    fun streamUserRoles(): Flow<List<UserRoleEntity>>

    @Upsert
    fun upsertRoles(roles: List<UserRoleEntity>)
}
