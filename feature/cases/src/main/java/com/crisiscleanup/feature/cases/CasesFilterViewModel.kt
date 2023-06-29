package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.commoncase.model.FormFieldNode
import com.crisiscleanup.core.commoncase.model.WorkFormGroupKey
import com.crisiscleanup.core.commoncase.model.flatten
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.model.data.CasesFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CasesFilterViewModel @Inject constructor(
    private val casesFilterRepository: CasesFilterRepository,
    incidentSelector: IncidentSelector,
    incidentsRepository: IncidentsRepository,
    languageRepository: LanguageTranslationsRepository,
    val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
) : ViewModel() {
    val casesFilter = casesFilterRepository.casesFilter

    val workTypes = incidentSelector.incidentId
        .flatMapLatest { id ->
            incidentsRepository.streamIncident(id)
        }
        .map { incident ->
            incident?.formFields?.let { formFields ->
                val formFieldRootNode = FormFieldNode.buildTree(
                    formFields,
                    languageRepository
                )
                    .map(FormFieldNode::flatten)

                return@map formFieldRootNode.firstOrNull { it.fieldKey == WorkFormGroupKey }
                    ?.let { node -> node.children.filter { it.parentKey == WorkFormGroupKey } }
                    ?: emptyList()
            }
            emptyList()
        }

    init {
        workTypes.onEach {
            logger.logDebug("Work types", workTypes)
        }
            .launchIn(viewModelScope)
    }

    fun changeFilters(filters: CasesFilter) {
        casesFilterRepository.changeFilter(filters)
    }
}