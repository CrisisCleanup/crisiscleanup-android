package com.crisiscleanup.core.database.model

data class WorksiteEntities(
    val core: WorksiteEntity,
    val flags: List<WorksiteFlagEntity>,
    val formData: List<WorksiteFormDataEntity>,
    val notes: List<WorksiteNoteEntity>,
    val workTypes: List<WorkTypeEntity>,
    val files: List<NetworkFileEntity> = emptyList(),
)
