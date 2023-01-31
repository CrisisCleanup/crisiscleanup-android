package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

/**
 * Keeps track of incident worksites syncing
 */
data class WorksitesSyncStats(
    val incidentId: Long,
    /**
     * Timestamp when the incident first started syncing
     */
    val syncStart: Instant?,
    /**
     * Number of worksites reported on first sync
     */
    val worksitesCount: Int,
    /**
     * Number of worksites pulled and saved locally during first sync
     */
    val pagedCount: Int = 0,
    /**
     * Sync attempt stats after the first full sync (of base worksite data)
     */
    val syncAttempt: SyncAttempt,
)
