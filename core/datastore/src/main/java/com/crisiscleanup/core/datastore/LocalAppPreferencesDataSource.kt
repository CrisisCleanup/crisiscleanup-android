package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentCoordinateBounds
import com.crisiscleanup.core.model.data.UserData
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.core.model.data.worksiteSortByFromLiteral
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Stores data and preferences related to the local app (on each device)
 */
class LocalAppPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val userData = userPreferences.data
        .map {
            UserData(
                darkThemeConfig = when (it.darkThemeConfig) {
                    null,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_UNSPECIFIED,
                    DarkThemeConfigProto.UNRECOGNIZED,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM,
                    ->
                        DarkThemeConfig.FOLLOW_SYSTEM

                    DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT ->
                        DarkThemeConfig.LIGHT

                    DarkThemeConfigProto.DARK_THEME_CONFIG_DARK -> DarkThemeConfig.DARK
                },
                shouldHideOnboarding = it.shouldHideOnboarding,

                selectedIncidentId = if (it.selectedIncidentId <= 0L) EmptyIncident.id else it.selectedIncidentId,

                languageKey = it.languageKey,

                tableViewSortBy = worksiteSortByFromLiteral(it.tableViewSortBy),

                allowAllAnalytics = it.allowAllAnalytics,

                hideGettingStartedVideo = it.hideGettingStartedVideo,

                isMenuTutorialDone = it.isMenuTutorialDone,

                shareLocationWithOrg = it.shareLocationWithOrg,

                casesMapBounds = it.casesMapBounds.asExternalModel(),
                teamMapBounds = it.teamMapBounds.asExternalModel(),

                isWorkScreenTableView = it.isWorkScreenTableView,

                isSyncMediaImmediate = it.syncMediaImmediate,
            )
        }

    suspend fun reset() {
        userPreferences.updateData {
            it.copy {
                this.darkThemeConfig = DarkThemeConfigProto.DARK_THEME_CONFIG_UNSPECIFIED
                shouldHideOnboarding = false
                syncAttempt = SyncAttemptProto.newBuilder().build()
                selectedIncidentId = EmptyIncident.id
                languageKey = ""
                tableViewSortBy = WorksiteSortBy.None.literal
                allowAllAnalytics = false
                hideGettingStartedVideo = false
                isMenuTutorialDone = false
                shareLocationWithOrg = false
                // TODO Bounds
                isWorkScreenTableView = false
            }
        }
    }

    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        userPreferences.updateData {
            it.copy {
                this.darkThemeConfig = when (darkThemeConfig) {
                    DarkThemeConfig.FOLLOW_SYSTEM ->
                        DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM

                    DarkThemeConfig.LIGHT -> DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT
                    DarkThemeConfig.DARK -> DarkThemeConfigProto.DARK_THEME_CONFIG_DARK
                }
            }
        }
    }

    suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        userPreferences.updateData {
            it.copy {
                this.shouldHideOnboarding = shouldHideOnboarding
            }
        }
    }

    suspend fun setSelectedIncident(id: Long) {
        userPreferences.updateData {
            it.copy { selectedIncidentId = id }
        }
    }

    suspend fun setLanguageKey(key: String) {
        userPreferences.updateData {
            it.copy { languageKey = key }
        }
    }

    suspend fun setTableViewSortBy(sortBy: WorksiteSortBy) {
        userPreferences.updateData {
            it.copy { tableViewSortBy = sortBy.literal }
        }
    }

    suspend fun setAnalytics(allowAll: Boolean) {
        userPreferences.updateData {
            it.copy { allowAllAnalytics = allowAll }
        }
    }

    suspend fun setHideGettingStartedVideo(hide: Boolean) {
        userPreferences.updateData {
            it.copy { hideGettingStartedVideo = hide }
        }
    }

    suspend fun setMenuTutorialDone(isDone: Boolean) {
        userPreferences.updateData {
            it.copy { isMenuTutorialDone = isDone }
        }
    }

    suspend fun setShareLocationWithOrg(share: Boolean) {
        userPreferences.updateData {
            it.copy { shareLocationWithOrg = share }
        }
    }

    suspend fun saveCasesMapBounds(bounds: IncidentCoordinateBounds) {
        userPreferences.updateData {
            it.copy { casesMapBounds = bounds.asProto() }
        }
    }

    suspend fun saveTeamMapBounds(bounds: IncidentCoordinateBounds) {
        userPreferences.updateData {
            it.copy { teamMapBounds = bounds.asProto() }
        }
    }

    suspend fun saveWorkScreenView(isTableView: Boolean) {
        userPreferences.updateData {
            it.copy { isWorkScreenTableView = isTableView }
        }
    }

    suspend fun saveSyncMediaImmediate(syncImmediate: Boolean) {
        userPreferences.updateData {
            it.copy { syncMediaImmediate = syncImmediate }
        }
    }
}

private fun IncidentMapBoundsProto.asExternalModel() = IncidentCoordinateBounds(
    incidentId,
    south = south,
    west = west,
    north = north,
    east = east,
)

private fun IncidentCoordinateBounds.asProto() = IncidentMapBoundsProto.newBuilder()
    .also {
        it.incidentId = incidentId
        it.north = north
        it.east = east
        it.south = south
        it.west = west
    }
    .build()
