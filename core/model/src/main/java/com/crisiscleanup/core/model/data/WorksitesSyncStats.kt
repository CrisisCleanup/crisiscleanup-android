package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

/**
 * Version of app where worksites (database) entity was last changed
 */
private const val WORKSITES_STABLE_MODEL_BUILD_VERSION = 33

/**
 * Keeps track of incident worksites syncing
 */
data class WorksitesSyncStats(
    val incidentId: Long,
    /**
     * Timestamp when the incident first started syncing
     *
     * See [syncAttempt] for last successful sync timestamp
     */
    val syncStart: Instant,
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

    val appBuildVersionCode: Long,
) {
    val isDataVersionOutdated = appBuildVersionCode < WORKSITES_STABLE_MODEL_BUILD_VERSION

    val shouldSync = pagedCount < worksitesCount ||
            isDataVersionOutdated ||
            syncAttempt.shouldSyncPassively()

}
