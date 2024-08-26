package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.combine
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.commoncase.model.FormFieldNode
import com.crisiscleanup.core.commoncase.model.WORK_FORM_GROUP_KEY
import com.crisiscleanup.core.commoncase.model.flatten
import com.crisiscleanup.core.data.IncidentRefresher
import com.crisiscleanup.core.data.LanguageRefresher
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
import com.google.android.gms.maps.model.LatLng
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

internal class CaseEditorDataLoader(
    private val isCreateWorksite: Boolean,
    incidentIdIn: Long,
    worksiteIdIn: Long?,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentRefresher: IncidentRefresher,
    incidentBoundsProvider: IncidentBoundsProvider,
    locationProvider: LocationProvider,
    worksitesRepository: WorksitesRepository,
    worksiteChangeRepository: WorksiteChangeRepository,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    workTypeStatusRepository: WorkTypeStatusRepository,
    translate: (String) -> String,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher,
    appEnv: AppEnv,
    private val logger: AppLogger,
    private val debugTag: String = "",
) {
    private val logDebug = appEnv.isDebuggable && debugTag.isNotBlank()

    val editSections = MutableStateFlow<List<String>>(emptyList())

    private val incidentFieldLookup = MutableStateFlow(emptyMap<String, GroupSummaryFieldLookup>())
    private val workTypeGroupChildrenLookup =
        MutableStateFlow(emptyMap<String, Collection<String>>())
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
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
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
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    private val worksiteIdStream = MutableStateFlow(worksiteIdIn)

    val worksiteStream = worksiteIdStream
        .flatMapLatest { worksiteId ->
            if (worksiteId == null || worksiteId <= 0) {
                flowOf(null)
            } else {
                worksitesRepository.streamLocalWorksite(worksiteId)
            }
        }
        .distinctUntilChanged()
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3.seconds.inWholeMilliseconds),
        )

    private val isInitiallySynced = AtomicBoolean(false)
    private val isWorksitePulled = MutableStateFlow(false)

    private val workTypeStatusStream = workTypeStatusRepository.workTypeStatusOptions

    private val viewStateInternal = combine(
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
                return@mapLatest CaseEditorViewState.Error(
                    errorMessage = translate("info.organization_issue_log_out"),
                )
            }

            val (incident, bounds) = incidentData

            if (!pullingIncident && incident.formFields.isEmpty()) {
                logger.logException(Exception("Incident $incidentIdIn is missing form fields when editing worksite $worksiteId"))
                val errorMessage = translate("info.incident_loading")
                    .replace("{name}", incident.name)
                return@mapLatest CaseEditorViewState.Error(
                    errorMessage = errorMessage,
                )
            }

            if (bounds.locations.isEmpty()) {
                logger.logException(Exception("Incident ${incident.id} ${incident.name} is lacking locations."))
                val errorMessage = translate("info.current_incident_problem")
                    .replace("{name}", incident.name)
                return@mapLatest CaseEditorViewState.Error(errorMessage = errorMessage)
            }

            val (localWorksite, isPulled) = third

            val loadedWorksite = localWorksite?.worksite
            var worksiteState = if (loadedWorksite == null) {
                var worksiteCoordinates = bounds.centroid
                locationProvider.coordinates?.let {
                    val deviceLocation = LatLng(it.first, it.second)
                    if (bounds.containsLocation(deviceLocation)) {
                        worksiteCoordinates = deviceLocation
                    }
                }

                EmptyWorksite.copy(
                    incidentId = incidentIdIn,
                    autoContactFrequencyT = AutoContactFrequency.NotOften.literal,
                    latitude = worksiteCoordinates.latitude,
                    longitude = worksiteCoordinates.longitude,
                    flags = EmptyWorksite.flags,
                )
            } else {
                loadedWorksite
            }

            with(editableWorksiteProvider) {
                this.incident = incident

                if ((loadedWorksite != null && takeStale()) || formFields.isEmpty()) {
                    formFields = FormFieldNode.buildTree(
                        incident.formFields,
                        languageRepository,
                    )
                        .map(FormFieldNode::flatten)

                    formFieldTranslationLookup = incident.formFields
                        .filter { it.fieldKey.isNotBlank() && it.label.isNotBlank() }
                        .associate { it.fieldKey to it.label }

                    val workTypeFormFields =
                        formFields.firstOrNull { it.fieldKey == WORK_FORM_GROUP_KEY }
                            ?.let { node -> node.children.filter { it.parentKey == WORK_FORM_GROUP_KEY } }
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

                    val textAreaLookup = incident.formFields
                        .filter(IncidentFormField::isTextArea)
                        .associateBy(IncidentFormField::fieldKey)
                    otherNotes = editableWorksite.mapNotNull { worksite ->
                        worksite.formData?.let { worksiteFormData ->
                            return@mapNotNull worksiteFormData
                                .filter { textAreaLookup.containsKey(it.key) }
                                .filter { it.value.valueString.isNotBlank() }
                                .map {
                                    val parentKey = textAreaLookup[it.key]!!.parentKey
                                    val groupLabel = translate("formLabels.$parentKey")
                                    val fieldLabel = translate("formLabels.${it.key}")
                                    val label = "$groupLabel - $fieldLabel"
                                    Pair(label, it.value.valueString.trim())
                                }
                                .sortedBy { it.first }
                        }
                    }

                    val localTranslate = { s: String -> translate(s) }
                    incidentFieldLookup.value = formFields.associate { node ->
                        val groupFieldMap = node.children.associate { child ->
                            child.fieldKey to child.formField.label.ifEmpty { localTranslate(child.fieldKey) }
                        }
                        val groupOptionsMap = node.children.map(FormFieldNode::options)
                            .flatMap { it.entries }
                            .associate { it.key to it.value }
                        node.fieldKey to GroupSummaryFieldLookup(
                            groupFieldMap,
                            groupOptionsMap,
                        )
                    }
                }

                if (editSections.value.isEmpty() && formFields.isNotEmpty()) {
                    editSections.value = mutableListOf<String>().apply {
                        add(translate("caseForm.property_information"))
                        val requiredGroups = setOf("workInfo")
                        addAll(
                            formFields.map {
                                with(it.formField) {
                                    val labelTranslateKey = "formLabels.$fieldKey"
                                    var translatedLabel = translate(labelTranslateKey)
                                    if (translatedLabel == labelTranslateKey) {
                                        translatedLabel = translate(fieldKey)
                                    }
                                    val isRequired = requiredGroups.contains(group)
                                    if (isRequired) {
                                        "$translatedLabel *"
                                    } else {
                                        translatedLabel
                                    }
                                }
                            },
                        )
                        add(translate("caseForm.photos"))
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
                if (updatedFormData.size != worksiteState.formData?.size) {
                    worksiteState = worksiteState.copy(
                        formData = updatedFormData,
                    )
                }

                if (!isStale || loadedWorksite != null) {
                    editableWorksite.value = worksiteState
                }
                incidentBounds = bounds
            }

            var isEditingAllowed = editSections.value.isNotEmpty() &&
                workTypeStatuses.isNotEmpty()
            var isNetworkLoadFinished = true
            var isLocalLoadFinished = true
            if (!isCreateWorksite) {
                // Minimal state for editing to to begin
                isEditingAllowed = isEditingAllowed &&
                    localWorksite != null
                isNetworkLoadFinished = isEditingAllowed &&
                    isPulled
                // Reliable state for editing to begin.
                // There are edge cases where network changes are still committing/propagating locally while this is true.
                // If internet connection is not available this may never turn true.
                isLocalLoadFinished = isNetworkLoadFinished &&
                    worksiteState.formData?.isNotEmpty() == true
            }
            val isTranslationUpdated =
                editableWorksiteProvider.formFieldTranslationLookup.isNotEmpty()
            CaseEditorViewState.CaseData(
                organization.id,
                isEditingAllowed,
                workTypeStatuses,
                worksiteState,
                incident,
                localWorksite,
                isNetworkLoadFinished = isNetworkLoadFinished,
                isLocalLoadFinished = isLocalLoadFinished,
                isTranslationUpdated = isTranslationUpdated,
            )
        }

    val viewState: MutableStateFlow<CaseEditorViewState> =
        MutableStateFlow(CaseEditorViewState.Loading)

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

        viewStateInternal
            .onEach { viewState.value = it }
            .launchIn(coroutineScope)
    }

    fun reloadData(worksiteId: Long) {
        editableWorksiteProvider.setStale()
        worksiteIdStream.value = worksiteId
        dataLoadCountStream.value++
    }
}
