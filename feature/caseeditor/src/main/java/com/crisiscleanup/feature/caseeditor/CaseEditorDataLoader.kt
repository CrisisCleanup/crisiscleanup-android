package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.IncidentBoundsProvider
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import com.crisiscleanup.feature.caseeditor.model.flatten
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class CaseEditorDataLoader(
    private val isCreateWorksite: Boolean,
    incidentIdIn: Long,
    worksiteIdIn: Long?,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    private val incidentRefresher: IncidentRefresher,
    incidentBoundsProvider: IncidentBoundsProvider,
    private val worksitesRepository: WorksitesRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    translate: (String) -> String,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    resourceProvider: AndroidResourceProvider,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher,
    appEnv: AppEnv,
    private val logger: AppLogger,
    private val debugTag: String = "",
) {
    private val logDebug = appEnv.isDebuggable && debugTag.isNotEmpty()

    val editSections = MutableStateFlow<List<String>>(emptyList())

    val incidentFieldLookup = MutableStateFlow(emptyMap<String, GroupSummaryFieldLookup>())
    val workTypeGroupChildrenLookup = MutableStateFlow(emptyMap<String, Collection<String>>())
    private var workTypeGroupFormFields = emptyMap<String, IncidentFormField>()

    private val dataLoadCountStream = MutableStateFlow(0)
    private val isRefreshingIncident = MutableStateFlow(false)
    private val isRefreshingWorksite = MutableStateFlow(false)

    val isLoading = combine(
        isRefreshingIncident,
        isRefreshingWorksite,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val organizationStream = accountDataRepository.accountData
        .mapLatest { it.org }
        .distinctUntilChanged()
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val incidentDataStream = incidentsRepository.streamIncident(incidentIdIn)
        .flatMapLatest { incident ->
            incident?.let {
                return@flatMapLatest incidentBoundsProvider.mapIncidentBounds(it)
                    .mapLatest { bounds ->
                        Pair(incident, bounds)
                    }
            }
            flowOf(null)
        }
        .flowOn(coroutineDispatcher)
        .distinctUntilChanged()
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val worksiteIdStream = MutableStateFlow(worksiteIdIn)

    val worksiteStream = worksiteIdStream
        .flatMapLatest { worksiteId ->
            if (worksiteId == null || worksiteId <= 0) flowOf(null)
            else worksitesRepository.streamLocalWorksite(worksiteId)
        }
        .distinctUntilChanged()
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val isInitiallySynced = AtomicBoolean(false)
    private val isWorksitePulled = MutableStateFlow(false)

    private val workTypeStatusStream = workTypeStatusRepository.workTypeStatusOptions

    private val _uiState = com.crisiscleanup.core.common.combine(
        dataLoadCountStream,
        organizationStream,
        workTypeStatusStream,
        incidentDataStream,
        isRefreshingIncident,
        worksiteStream,
        isWorksitePulled,
    ) {
            dataLoadCount, organization, statuses,
            incidentData, pullingIncident,
            worksite, isPulled,
        ->
        Triple(
            Triple(dataLoadCount, organization, statuses),
            Pair(incidentData, pullingIncident),
            Pair(worksite, isPulled),
        )
    }
        .filter { (first, second, _) ->
            val (_, organization, _) = first
            val (incidentData, _) = second
            organization != null && incidentData != null
        }
        .mapLatest { (first, second, third) ->
            val (_, organization, workTypeStatuses) = first
            val (incidentData, pullingIncident) = second

            organization!!
            incidentData!!

            val worksiteId = worksiteIdStream.first() ?: -1

            if (organization.id <= 0) {
                logger.logException(Exception("Organization $organization is not set when editing worksite $worksiteId"))
                return@mapLatest CaseEditorUiState.Error(R.string.organization_issue_try_re_authenticating)
            }

            val (incident, bounds) = incidentData

            if (!pullingIncident && incident.formFields.isEmpty()) {
                logger.logException(Exception("Incident $incidentIdIn is missing form fields when editing worksite $worksiteId"))
                return@mapLatest CaseEditorUiState.Error(R.string.incident_issue_try_again)
            }

            if (bounds.locations.isEmpty()) {
                logger.logException(Exception("Incident ${incident.id} ${incident.name} is lacking locations."))
                val errorMessage = resourceProvider.getString(
                    R.string.incident_is_in_progress,
                    incident.name,
                )
                return@mapLatest CaseEditorUiState.Error(errorMessage = errorMessage)
            }

            val (localWorksite, isPulled) = third

            val loadedWorksite = localWorksite?.worksite
            var worksiteState = loadedWorksite ?: EmptyWorksite.copy(
                incidentId = incidentIdIn,
                autoContactFrequencyT = AutoContactFrequency.Often.literal,
            )

            with(editableWorksiteProvider) {
                this.incident = incident

                if ((loadedWorksite != null && takeStale()) || formFields.isEmpty()) {
                    formFields = FormFieldNode.buildTree(
                        incident.formFields,
                        languageRepository
                    )
                        .map(FormFieldNode::flatten)

                    formFieldTranslationLookup = incident.formFields
                        .filter { it.fieldKey.isNotBlank() && it.label.isNotBlank() }
                        .associate { it.fieldKey to it.label }

                    val workTypeFormFields =
                        formFields.firstOrNull { it.fieldKey == WorkFormGroupKey }
                            ?.let { node -> node.children.filter { it.parentKey == WorkFormGroupKey } }
                            ?: emptyList()

                    workTypeGroupChildrenLookup.value = workTypeFormFields.associate {
                        it.fieldKey to it.children.map(FormFieldNode::fieldKey).toSet()
                    }
                    workTypeGroupFormFields = workTypeFormFields.associate {
                        val formField = incident.formFieldLookup[it.fieldKey]!!
                        formField.selectToggleWorkType to formField
                    }

                    workTypeTranslationLookup = workTypeFormFields.associate {
                        val name = formFieldTranslationLookup[it.fieldKey] ?: it.fieldKey
                        it.formField.selectToggleWorkType to name
                    }

                    val localTranslate = { s: String -> translate(s) }
                    incidentFieldLookup.value = formFields.associate { node ->
                        val groupFieldMap = node.children.associate { child ->
                            child.fieldKey to child.formField.getFieldLabel(localTranslate)
                        }
                        val groupOptionsMap = node.children.map(FormFieldNode::options)
                            .flatMap { it.entries }
                            .associate { it.key to it.value }
                        node.fieldKey to GroupSummaryFieldLookup(
                            groupFieldMap,
                            groupOptionsMap,
                        )
                    }

                    editSections.value = mutableListOf<String>().apply {
                        add(translate("caseForm.property_information"))
                        val requiredGroups = setOf("workInfo")
                        addAll(formFields.map {
                            with(it.formField) {
                                val isRequired = requiredGroups.contains(group)
                                if (isRequired) "$label *"
                                else label
                            }
                        })
                    }
                }

                val updatedFormData = worksiteState.formData?.toMutableMap() ?: mutableMapOf()
                // Set work type groups where child has value
                val workTypeGroups = updatedFormData.keys
                    .filter { incident.workTypeLookup[it] != null }
                    .mapNotNull { incident.formFieldLookup[it]?.parentKey }
                    .toSet()
                if (workTypeGroups.isNotEmpty()) {
                    workTypeGroups.forEach {
                        updatedFormData[it] = WorksiteFormValue.trueValue
                    }
                }
                // Set work type group where work type is defined
                worksiteState.workTypes.forEach {
                    workTypeGroupFormFields[it.workTypeLiteral]?.let { formField ->
                        updatedFormData[formField.fieldKey] = WorksiteFormValue.trueValue
                    }
                }
                if (updatedFormData.size != (worksiteState.formData?.size ?: 0)) {
                    worksiteState = worksiteState.copy(
                        formData = updatedFormData,
                    )
                }

                if (!isStale || loadedWorksite != null) {
                    editableWorksite.value = worksiteState
                }
                incidentBounds = bounds
            }

            val isReadyForEditing = editSections.value.isNotEmpty() &&
                    workTypeStatuses.isNotEmpty() &&
                    (isCreateWorksite || localWorksite != null)
            val isNetworkLoadFinished = isReadyForEditing && (isCreateWorksite || isPulled)
            // TODO Revisit after worksite caching is complete
            //      Data should not be editable until this is finished
            //      Once this is finished new changes should be ignored until changes are committed
            val isLocalLoadFinished = isNetworkLoadFinished &&
                    (isCreateWorksite || worksiteState.phone1.isNotBlank())
            val isTranslationUpdated =
                editableWorksiteProvider.formFieldTranslationLookup.isNotEmpty()
            CaseEditorUiState.CaseData(
                organization.id,
                isReadyForEditing,
                workTypeStatuses,
                worksiteState,
                incident,
                localWorksite,
                isNetworkLoadFinished = isNetworkLoadFinished,
                isLocalLoadFinished = isLocalLoadFinished,
                isTranslationUpdated = isTranslationUpdated,
            )
        }

    val uiState: MutableStateFlow<CaseEditorUiState> = MutableStateFlow(CaseEditorUiState.Loading)

    init {
        if (logDebug) {
            (logger as? TagLogger)?.let {
                it.tag = debugTag
            }
        }

        coroutineScope.launch(coroutineDispatcher) {
            try {
                languageRefresher.pullLanguages()
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        coroutineScope.launch(coroutineDispatcher) {
            try {
                workTypeStatusRepository.loadStatuses()
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        coroutineScope.launch(coroutineDispatcher) {
            isRefreshingIncident.value = true
            try {
                incidentRefresher.pullIncident(incidentIdIn)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isRefreshingIncident.value = false
            }
        }

        worksiteStream
            .onEach {
                it?.let { localWorksite ->
                    if (isInitiallySynced.getAndSet(true)) {
                        return@onEach
                    }

                    try {
                        val worksite = localWorksite.worksite
                        val networkId = worksite.networkId
                        if (worksite.id > 0 &&
                            (networkId > 0 || localWorksite.localChanges.isLocalModified)
                        ) {
                            isRefreshingWorksite.value = true
                            if (worksiteChangeRepository.trySyncWorksite(worksite.id) &&
                                networkId > 0
                            ) {
                                worksitesRepository.pullWorkTypeRequests(networkId)
                            }
                        }
                    } finally {
                        isRefreshingWorksite.value = false
                        isWorksitePulled.value = true
                    }
                }
            }
            .flowOn(coroutineDispatcher)
            .launchIn(coroutineScope)

        _uiState
            .onEach { uiState.value = it }
            .launchIn(coroutineScope)
    }

    fun reloadData(worksiteId: Long) {
        editableWorksiteProvider.setStale()
        worksiteIdStream.value = worksiteId
        dataLoadCountStream.value++
    }
}