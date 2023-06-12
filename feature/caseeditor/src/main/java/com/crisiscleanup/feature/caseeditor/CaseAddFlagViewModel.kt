package com.crisiscleanup.feature.caseeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.feature.caseeditor.model.coordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseAddFlagViewModel @Inject constructor(
    editableWorksiteProvider: EditableWorksiteProvider,
    organizationsRepository: OrganizationsRepository,
    private val accountDataRepository: AccountDataRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val syncPusher: SyncPusher,
    val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    @ApplicationScope private val externalScope: CoroutineScope,
) : ViewModel() {
    private val worksiteIn = editableWorksiteProvider.editableWorksite.value
    private val flagsIn =
        worksiteIn.flags?.mapNotNull(WorksiteFlag::flagType)?.toSet() ?: emptySet()

    private val allFlag = listOf(
        WorksiteFlagType.HighPriority,
        WorksiteFlagType.UpsetClient,
        WorksiteFlagType.MarkForDeletion,
        WorksiteFlagType.ReportAbuse,
        WorksiteFlagType.Duplicate,
        WorksiteFlagType.WrongLocation,
        WorksiteFlagType.WrongIncident,
    )
    private val singleExistingFlags = setOf(
        WorksiteFlagType.HighPriority,
        WorksiteFlagType.MarkForDeletion,
        WorksiteFlagType.Duplicate,
    )
        .toSet()

    val flagFlows = flowOf(flagsIn).mapLatest { existingFlags ->
        val existingSingularFlags = existingFlags
            .filter { singleExistingFlags.contains(it) }
            .toSet()

        allFlag.filter { !existingSingularFlags.contains(it) }
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val isSaving = MutableStateFlow(false)
    val isSaved = MutableStateFlow(false)
    val isEditable = combine(
        isSaving,
        isSaved,
        ::Pair,
    )
        .mapLatest { (b0, b1) -> !(b0 || b1) }
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

    private fun commitFlag(
        flag: WorksiteFlag,
        overwrite: Boolean = false,
        onPostSave: () -> Unit = { isSaved.value = true },
    ) {
        flag.flagType?.let { flagType ->
            if (flagsIn.contains(flagType) && !overwrite) {
                onPostSave()
                return
            }

            viewModelScope.launch(ioDispatcher) {
                isSaving.value = true
                try {
                    saveFlag(flag, flagType)
                    onPostSave()
                } catch (e: Exception) {
                    // TODO Show error
                    logger.logException(e)
                } finally {
                    isSaving.value = false
                }
            }
        }
    }

    // TODO Test coverage. Especially overwriting
    private suspend fun saveFlag(
        flag: WorksiteFlag,
        flagType: WorksiteFlagType,
    ) {
        val organizationId = accountDataRepository.accountData.first().org.id

        var currentFlags = worksiteIn.flags ?: emptyList()

        val primaryWorkType = worksiteIn.keyWorkType ?: worksiteIn.workTypes.first()

        if (flagsIn.contains(flagType)) {
            val flagsDeleted = currentFlags.filter { it.reasonT == flagType.literal }
            if (flagsDeleted.size < currentFlags.size) {
                worksiteChangeRepository.saveWorksiteChange(
                    worksiteIn,
                    worksiteIn.copy(flags = flagsDeleted),
                    primaryWorkType,
                    organizationId,
                )
                currentFlags = flagsDeleted
            }
        }

        val flagAdded = currentFlags.toMutableList().apply {
            add(flag)
        }
        worksiteChangeRepository.saveWorksiteChange(
            worksiteIn,
            worksiteIn.copy(flags = flagAdded),
            primaryWorkType,
            organizationId,
        )

        externalScope.launch {
            syncPusher.appPushWorksite(worksiteIn.id)
        }
    }

    fun onHighPriority(isHighPriority: Boolean, notes: String) {
        val highPriorityFlag = WorksiteFlag.flag(
            WorksiteFlagType.HighPriority,
            notes,
            isHighPriority,
        )
        commitFlag(highPriorityFlag)
    }

    fun onUpsetClient(notes: String) {

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
