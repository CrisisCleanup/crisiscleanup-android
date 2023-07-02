package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.KeyResourceTranslator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CaseHistoryViewModel @Inject constructor(
    editableWorksiteProvider: EditableWorksiteProvider,
    val translator: KeyResourceTranslator,
) : ViewModel() {
    private val worksiteIn = editableWorksiteProvider.editableWorksite.value
}