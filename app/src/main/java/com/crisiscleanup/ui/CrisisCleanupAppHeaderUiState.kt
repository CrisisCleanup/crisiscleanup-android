package com.crisiscleanup.ui

import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.data.IncidentSelectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupAppHeaderUiState @Inject constructor(
    incidentSelectManager: IncidentSelectManager,
    @ApplicationScope scope: CoroutineScope,
) : AppHeaderUiState {
    override var title = MutableStateFlow("")
        private set

    init {
        incidentSelectManager.incident.onEach {
            setTitle(it.shortName)
        }
            .launchIn(scope)
    }

    override fun setTitle(title: String) {
        this.title.value = title
    }
}
