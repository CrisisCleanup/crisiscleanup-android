package com.crisiscleanup.core.model.data

data class LocalWorksite(
    val worksite: Worksite,
    val localImages: List<WorksiteLocalImage>,
    val localChanges: LocalChange,
)
