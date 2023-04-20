package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

/**
 * Version of app where worksites (database) entity was last changed
 */
private const val WorksitesStableModelBuildVersion = 33
const val IncidentOrganizationsStableModelBuildVersion = 61

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
    val isDataVersionOutdated = appBuildVersionCode < stableModelVersion

    val shouldSync = pagedCount < dataCount ||
            isDataVersionOutdated ||
            syncAttempt.shouldSyncPassively()
}
