package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.WorksiteChangeDaoPlus
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import javax.inject.Inject

interface WorksiteChangeRepository {
    suspend fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long,
    ): Long
}

class CrisisCleanupWorksiteChangeRepository @Inject constructor(
    private val worksiteChangeDaoPlus: WorksiteChangeDaoPlus,
) : WorksiteChangeRepository {
    override suspend fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long
    ) = worksiteChangeDaoPlus.saveChange(
        worksiteStart,
        worksiteChange,
        primaryWorkType,
        organizationId,
    )
}
