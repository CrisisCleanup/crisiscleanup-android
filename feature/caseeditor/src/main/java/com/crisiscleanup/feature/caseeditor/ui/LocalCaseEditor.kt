package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.crisiscleanup.core.model.data.WorkTypeStatus

data class CaseEditor(
    val statusOptions: List<WorkTypeStatus>
)

private val caseEditor = CaseEditor(
    statusOptions = emptyList()
)

val LocalCaseEditor = staticCompositionLocalOf { caseEditor }