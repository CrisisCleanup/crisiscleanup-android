package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity

@Dao
interface WorksiteFormDataDao {
    @Transaction
    @Query(
        """
        DELETE FROM worksite_form_data
        WHERE worksite_id=:worksiteId AND field_key NOT IN(:fieldKeys)
        """
    )
    fun syncDeleteUnspecified(worksiteId: Long, fieldKeys: Collection<String>)

    @Upsert
    fun syncUpsert(formData: Collection<WorksiteFormDataEntity>)
}