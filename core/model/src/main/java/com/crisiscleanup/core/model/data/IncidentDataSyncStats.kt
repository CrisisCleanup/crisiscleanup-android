package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

/**
 * Build version of the app where worksite (related) entity models were last changed
 */
private const val WorksitesStableModelBuildVersion = 69

/**
 * Build version of the app where incident organization (related) entity models were last changed
 */
const val IncidentOrganizationsStableModelBuildVersion = 69

/**
 * Keeps track of incident data (worksites, organizations, ...) syncing
 */
data class IncidentDataSyncStats(
    val incidentId: Long,
    /**
     * Timestamp when the incident first started syncing
     *
     * See [syncAttempt] for last successful sync timestamp
     */
    val syncStart: Instant,
    /**
     * Number of (worksites, organizations, ...) reported on first sync
     */
    val dataCount: Int,
    /**
     * Number of data (pages) pulled and saved locally during first sync
     */
    val pagedCount: Int = 0,
    /**
     * Sync attempt stats after the first full sync (of base data)
     */
    val syncAttempt: SyncAttempt,

    val appBuildVersionCode: Long,
    /**
     * App build version where the network data model was last changed
     */
    private val stableModelVersion: Int = WorksitesStableModelBuildVersion,
) {
    /**
     * TRUE if the underlying worksite model has changed since the incident was last synced
     */
    val isDataVersionOutdated = appBuildVersionCode < stableModelVersion

    val shouldSync = pagedCount < dataCount ||
            isDataVersionOutdated ||
            syncAttempt.shouldSyncPassively()
}
