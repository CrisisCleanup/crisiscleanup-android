package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.mapmarker.model.IncidentBounds
import com.crisiscleanup.core.mapmarker.util.toBounds
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.feature.caseeditor.model.FormFieldNode
import com.crisiscleanup.feature.caseeditor.model.flatten
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class CaseEditorDataLoader(
    private val isCreateWorksite: Boolean,
    private val incidentIdIn: Long,
    worksiteIdIn: Long?,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    private val incidentRefresher: IncidentRefresher,
    locationsRepository: LocationsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val networkMonitor: NetworkMonitor,
    languageRepository: LanguageTranslationsRepository,
    languageRefresher: LanguageRefresher,
    translate: (String) -> String,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher,
    private val logger: AppLogger,
) {
    val incidentFieldLookup = MutableStateFlow(emptyMap<String, GroupSummaryFieldLookup>())
    val workTypeGroupChildrenLookup = MutableStateFlow(emptyMap<String, Collection<String>>())

    private val dataLoadCountStream = MutableStateFlow(0)
    val isRefreshingIncident = MutableStateFlow(false)
    val isRefreshingWorksite = MutableStateFlow(false)

    private val organizationStream = accountDataRepository.accountData
        .map { it.org }
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val incidentStream = incidentsRepository.streamIncident(incidentIdIn)
        .mapLatest { it ?: EmptyIncident }
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val incidentBoundsStream = incidentStream
        .mapLatest {
            var bounds: IncidentBounds? = null
            it?.locations?.map(IncidentLocation::location)?.let { locationIds ->
                val locations = locationsRepository.getLocations(locationIds).toLatLng()
                bounds = if (locations.isEmpty()) null
                else locations.toBounds()
            }
            bounds
        }
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val worksiteIdStream = MutableStateFlow(worksiteIdIn)

    val worksiteStream = worksiteIdStream
        .flatMapLatest { worksiteId ->
            if (worksiteId == null) flowOf(null)
            else worksitesRepository.streamLocalWorksite(worksiteId)
        }
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val isWorksitePulled = AtomicBoolean(false)
    private val networkWorksiteSync = AtomicReference<Pair<Long, NetworkWorksiteFull>?>(null)
    private val networkWorksiteStream = worksiteStream
        .mapLatest { cachedWorksite ->
            cachedWorksite?.let { localWorksite ->
                val networkId = localWorksite.worksite.networkId
                if (networkId > 0 &&
                    !isWorksitePulled.getAndSet(true)
                ) {
                    if (networkMonitor.isOnline.first()) {
                        refreshWorksite(networkId)
                    }
                }
            }
            networkWorksiteSync.get()
        }
        .flowOn(coroutineDispatcher)
        .stateIn(
            scope = coroutineScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    private val _uiState = com.crisiscleanup.core.common.combine(
        dataLoadCountStream,
        organizationStream,
        incidentStream,
        incidentBoundsStream,
        isRefreshingIncident,
        worksiteStream,
        networkWorksiteStream,
    ) {
            dataLoadCount, organization,
            incident, bounds, pullingIncident,
            worksite, networkWorksiteSync,
        ->
        Triple(
            Pair(dataLoadCount, organization),
            Triple(incident, bounds, pullingIncident),
            Pair(worksite, networkWorksiteSync),
        )
    }
        .mapLatest { (first, second, third) ->
            val (_, organization) = first
            val (incident, bounds, pullingIncident) = second

            if (organization == null || incident == null) {
                return@mapLatest CaseEditorUiState.Loading
            }

            val worksiteId = worksiteIdStream.first() ?: -1

            if (organization.id <= 0) {
                logger.logException(Exception("Organization $organization is not set when editing worksite $worksiteId"))
                return@mapLatest CaseEditorUiState.Error(R.string.organization_issue_try_re_authenticating)
            }

            if (!pullingIncident && incident.formFields.isEmpty()) {
                logger.logException(Exception("Incident $incidentIdIn is missing form fields when editing worksite $worksiteId"))
                return@mapLatest CaseEditorUiState.Error(R.string.incident_issue_try_again)
            }

            bounds?.let {
                if (it.locations.isEmpty()) {
                    logger.logException(Exception("Incident $incidentIdIn is lacking locations."))
                    return@mapLatest CaseEditorUiState.Error(R.string.incident_issue_try_again)
                }
            }

            val (localWorksite, networkWorksiteSync) = third

            val loadedWorksite = localWorksite?.worksite
            var initialWorksite = loadedWorksite ?: EmptyWorksite.copy(
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

                    formFieldTranslationLookup =
                        incident.formFields
                            .filter { it.fieldKey.isNotBlank() && it.label.isNotBlank() }
                            .associate { it.fieldKey to it.label }

                    workTypeGroupChildrenLookup.value =
                        formFields.firstOrNull { it.fieldKey == WorkFormGroupKey }
                            ?.let { node ->
                                node.children
                                    .filter { it.parentKey == WorkFormGroupKey }
                                    .associate {
                                        it.fieldKey to it.children.map(FormFieldNode::fieldKey)
                                    }
                            }
                            ?: emptyMap()

                    val localTranslate = { s: String -> translate(s) }
                    incidentFieldLookup.value = formFields.associate { node ->
                        val groupFieldMap = node.children.associate { child ->
                            child.fieldKey to child.formField.getFieldLabel(localTranslate)
                        }
                        val groupOptionsMap =
                            node.children.map(FormFieldNode::options)
                                .flatMap { it.entries }
                                .associate { it.key to it.value }
                        node.fieldKey to GroupSummaryFieldLookup(
                            groupFieldMap,
                            groupOptionsMap,
                        )
                    }
                }

                val updatedFormData = initialWorksite.formData?.toMutableMap() ?: mutableMapOf()
                val workTypeGroups = updatedFormData.keys
                    .filter { incident.workTypeLookup[it] != null }
                    .mapNotNull { incident.formFieldLookup[it]?.parentKey }
                    .toSet()
                if (workTypeGroups.isNotEmpty()) {
                    workTypeGroups.onEach {
                        updatedFormData[it] = WorksiteFormValue(true, "", true)
                    }
                }
                if (updatedFormData.size != (initialWorksite.formData?.size ?: 0) ||
                    initialWorksite.favoriteId != null
                ) {
                    initialWorksite = initialWorksite.copy(
                        formData = updatedFormData,
                        isAssignedToOrgMember = initialWorksite.favoriteId != null,
                    )
                }

                if (!isStale || loadedWorksite != null) {
                    editableWorksite.value = initialWorksite
                }
                incidentBounds = bounds ?: DefaultIncidentBounds
            }

            val isLoadFinished = isCreateWorksite ||
                    (!pullingIncident &&
                            localWorksite != null &&
                            isWorksitePulled.get())
            val isEditable = bounds != null && isLoadFinished
            CaseEditorUiState.WorksiteData(
                organization.id,
                isEditable,
                initialWorksite,
                incident,
                localWorksite,
                networkWorksiteSync,
            )
        }

    val uiState: MutableStateFlow<CaseEditorUiState> = MutableStateFlow(CaseEditorUiState.Loading)

    init {
        coroutineScope.launch(coroutineDispatcher) {
            try {
                languageRefresher.pullLanguages()
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

        _uiState
            .onEach { uiState.value = it }
            .launchIn(coroutineScope)
    }

    fun reloadData(worksiteId: Long) {
        editableWorksiteProvider.setStale()
        worksiteIdStream.value = worksiteId
        dataLoadCountStream.value++
    }

    private suspend fun refreshWorksite(networkWorksiteId: Long) {
        isRefreshingWorksite.value = true
        try {
            networkWorksiteSync.set(
                worksitesRepository.syncWorksite(
                    incidentIdIn,
                    networkWorksiteId,
                )
            )

            // TODO Try and merge changes if exists.
            //      If not show message that local changes have deviated from backend.
            // val isLocalModified = cachedWorksite.localChanges.isLocalModified
        } catch (e: Exception) {
            // TODO This is going to be difficult. Plenty of state for possible change... Show error message that backend has changes not resolved on local?
            logger.logException(e)
        } finally {
            isRefreshingWorksite.value = false
        }
    }
}