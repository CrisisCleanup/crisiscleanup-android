package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.KeyResourceTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class CaseAddFlagViewModel @Inject constructor(
    editableWorksiteProvider: EditableWorksiteProvider,
    val translator: KeyResourceTranslator,
) : ViewModel() {
    val flagFlows = MutableStateFlow(
        listOf(
            CaseFlagFlow.UpsetClient,
            CaseFlagFlow.MarkForDeletion,
            CaseFlagFlow.ReportAbuse,
            CaseFlagFlow.Duplicate,
            CaseFlagFlow.WrongLocation,
            CaseFlagFlow.WrongIncident,
        )
    )
}

enum class CaseFlagFlow(val translateKey: String) {
    None("flag.choose_problem"),
    UpsetClient("flag.worksite_upset_client"),
    MarkForDeletion("flag.worksite_mark_for_deletion"),
    ReportAbuse("flag.worksite_abuse"),
    Duplicate("flag.duplicate"),
    WrongLocation("flag.worksite_wrong_location"),
    WrongIncident("Wrong Incident"),
}