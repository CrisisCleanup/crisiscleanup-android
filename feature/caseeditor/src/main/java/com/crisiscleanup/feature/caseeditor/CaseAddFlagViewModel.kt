package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.feature.caseeditor.model.coordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CaseAddFlagViewModel @Inject constructor(
    editableWorksiteProvider: EditableWorksiteProvider,
    organizationsRepository: OrganizationsRepository,
    val translator: KeyResourceTranslator,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val flagFlows = MutableStateFlow(
        listOf(
            CaseFlagFlow.HighPriority,
            CaseFlagFlow.UpsetClient,
            CaseFlagFlow.MarkForDeletion,
            CaseFlagFlow.ReportAbuse,
            CaseFlagFlow.Duplicate,
            CaseFlagFlow.WrongLocation,
            CaseFlagFlow.WrongIncident,
        )
    )

    val isSaving = MutableStateFlow(false)
    val isEditable = isSaving.mapLatest(Boolean::not)
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val nearbyOrganizations = editableWorksiteProvider.editableWorksite.mapLatest {
        val coordinates = it.coordinates()
        organizationsRepository.getNearbyClaimingOrganizations(
            coordinates.latitude,
            coordinates.longitude,
        )
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    fun onHighPriority(reason: String) {

    }

    fun onUpsetClient() {

    }

    fun onMarkForDelete() {

    }

    fun onReportAbuse() {

    }

    fun onDuplicate() {

    }

    fun onWrongLocation() {

    }

    fun onWrongIncident() {

    }
}

enum class CaseFlagFlow(val translateKey: String) {
    None("flag.choose_problem"),
    HighPriority("flag.worksite_high_priority"),
    UpsetClient("flag.worksite_upset_client"),
    MarkForDeletion("flag.worksite_mark_for_deletion"),
    ReportAbuse("flag.worksite_abuse"),
    Duplicate("flag.duplicate"),
    WrongLocation("flag.worksite_wrong_location"),
    WrongIncident("Wrong Incident"),
}