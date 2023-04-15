package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class SavedWorksiteChange(
    val id: Long,
    val syncUuid: String,
    val createdAt: Instant,
    val organizationId: Long,
    val worksiteId: Long,
    val dataVersion: Int,
    val serializedData: String,
    private val saveAttempt: Int,
    private val archiveActionLiteral: String,
    private val stopSyncing: Boolean,
) {
    private val archiveAction = when (archiveActionLiteral) {
        WorksiteChangeArchiveAction.Synced.literal -> WorksiteChangeArchiveAction.Synced
        WorksiteChangeArchiveAction.PartiallySynced.literal -> WorksiteChangeArchiveAction.PartiallySynced
        else -> WorksiteChangeArchiveAction.Pending
    }

    val isSynced = archiveAction == WorksiteChangeArchiveAction.Synced
    val isPartiallySynced = archiveAction == WorksiteChangeArchiveAction.PartiallySynced

    val isArchived = stopSyncing || isSynced
}

enum class WorksiteChangeArchiveAction(val literal: String) {
    // Pending sync
    Pending(""),

    // Synced successfully
    Synced("synced"),

    // Worksite was synced but not all additional data was synced
    PartiallySynced("partially_synced"),
}