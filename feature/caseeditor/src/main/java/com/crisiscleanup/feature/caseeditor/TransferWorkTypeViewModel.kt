package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.commoncase.TransferWorkTypeProvider
import com.crisiscleanup.core.commoncase.WorkTypeTransferType.*
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.feature.caseeditor.model.contactList
import com.crisiscleanup.feature.caseeditor.navigation.TransferWorkTypeArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferWorkTypeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    organizationsRepository: OrganizationsRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val worksiteProvider: WorksiteProvider,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    private val transferWorkTypeProvider: TransferWorkTypeProvider,
    val translator: KeyResourceTranslator,
    private val syncPusher: SyncPusher,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel(), KeyResourceTranslator {
    private val transferWorkTypeArgs = TransferWorkTypeArgs(savedStateHandle)
    val transferType = transferWorkTypeProvider.transferType
    private val isFromCaseEdit = transferWorkTypeArgs.isFromCaseEdit

    val isTransferable = transferType != None && transferWorkTypeProvider.workTypes.isNotEmpty()
    private val organizationId = transferWorkTypeProvider.organizationId

    val screenTitle = when (transferType) {
        Release -> translate("actions.release")
        Request -> translate("workTypeRequestModal.work_type_request")
        else -> ""
    }

    val isTransferred = MutableStateFlow(false)
    val isTransferring = MutableStateFlow(false)

    val transferWorkTypesState = transferWorkTypeProvider.workTypes
    val workTypesState = mutableStateMapOf<Long, Boolean>()
        .also { map ->
            val isSingleWorkType = transferWorkTypesState.size == 1
            transferWorkTypesState.forEach { map[it.key.id] = it.value || isSingleWorkType }
        }

    var transferReason by mutableStateOf("")

    val reasonHint =
        if (transferType == Request) translate("workTypeRequestModal.reason_requested")
        else null

    val errorMessageReason = MutableStateFlow("")
    private val errorMessageWorkType = MutableStateFlow("")
    val errorMessage = combine(
        errorMessageReason,
        errorMessageWorkType,
    ) { s0, s1 -> s0.ifBlank { s1 } }
        .debounce(100)
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    private val requestWorkTypesState =
        organizationsRepository.organizationLookup.map { orgLookup ->
            val orgNameLookup = transferWorkTypesState
                .mapNotNull { (workType, _) ->
                    orgLookup[workType.orgClaim]?.let { org ->
                        workType.id to org.name
                    }
                }
                .associate { it.first to it.second }
            val contactLookup = transferWorkTypesState
                .mapNotNull { (workType, _) ->
                    orgLookup[workType.orgClaim]?.let { org ->
                        org.id to org.contactList
                    }
                }
                .associate { it.first to it.second }
            RequestWorkTypeState(orgNameLookup, contactLookup)
        }
            .stateIn(
                scope = viewModelScope,
                initialValue = RequestWorkTypeState(),
                started = SharingStarted.WhileSubscribed(),
            )

    val requestDescription = MutableStateFlow("")
    val contactList = requestWorkTypesState.mapLatest { workTypeState ->
        val contactListLookup = workTypeState.orgIdContactListLookup
        transferWorkTypesState.mapNotNull { it.key.orgClaim }
            .toSet()
            .mapNotNull { contactListLookup[it] }
            .flatten()
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    private val editableWorksite =
        if (isFromCaseEdit) editableWorksiteProvider.editableWorksite
        else worksiteProvider.editableWorksite

    init {
        transferWorkTypeProvider.clearPendingTransfer()

        requestWorkTypesState
            .onEach { updateRequestInfo() }
            .launchIn(viewModelScope)
    }

    fun updateRequestInfo() {
        if (transferType == Request) {
            with(transferWorkTypeProvider) {
                val orgLookup = requestWorkTypesState.value.workTypeIdOrgNameLookup
                val otherOrganizations = workTypesState
                    .filter { it.value }
                    .map { it.key }
                    .map { orgLookup[it] }
                    .toSet()
                    .joinToString(", ")
                requestDescription.value =
                    translate("workTypeRequestModal.request_modal_instructions")
                        .replace("{organizations}", otherOrganizations)
                        .replace("{my_organization}", organizationName)
                        .replace("{case_number}", caseNumber)

            }
        }
    }

    fun clearErrorMessage() {
        errorMessageReason.value = ""
        errorMessageWorkType.value = ""
    }

    fun commitTransfer(): Boolean {
        if (transferReason.isBlank()) {
            val isRelease = transferType == Release
            val reasonTranslateKey =
                if (isRelease) "workTypeRequestModal.explain_release_case_required"
                else "workTypeRequestModal.explain_request_case_required"
            errorMessageReason.value = translate(reasonTranslateKey)
        }

        if (workTypesState.filter { it.value }.isEmpty()) {
            errorMessageWorkType.value =
                translate("workTypeRequestModal.transfer_work_type_is_required")
        }

        if (errorMessageReason.value.isBlank() &&
            errorMessageWorkType.value.isBlank()
        ) {
            transferWorkTypes()
            return true
        }

        return false
    }

    private fun transferWorkTypes() {
        viewModelScope.launch(ioDispatcher) {
            isTransferring.value = true
            val isRequest = transferType == Request
            val workTypeIdLookup = transferWorkTypesState.keys
                .map { it.id to it.workTypeLiteral }
                .associate { it.first to it.second }
            val workTypes = workTypesState.mapNotNull {
                if (it.value) workTypeIdLookup[it.key]
                else null
            }
            val worksite = editableWorksite.value
            try {
                worksiteChangeRepository.saveWorkTypeTransfer(
                    worksite,
                    organizationId,
                    if (isRequest) transferReason else "",
                    if (isRequest) workTypes else emptyList(),
                    if (isRequest) "" else transferReason,
                    if (isRequest) emptyList() else workTypes,
                )

                syncPusher.appPushWorksite(worksite.id)

                isTransferred.value = true
            } catch (e: Exception) {
                // TODO Show error
                logger.logException(e)
            } finally {
                isTransferring.value = false
            }
        }
    }

    // KeyResourceTranslator

    override val translationCount = translator.translationCount

    override fun translate(phraseKey: String) = translate(phraseKey, 0)

    override fun translate(phraseKey: String, fallbackResId: Int): String {
        val translated = if (isFromCaseEdit) editableWorksiteProvider.translate(phraseKey)
        else worksiteProvider.translate(phraseKey)
        return translated ?: translator.translate(
            phraseKey,
            fallbackResId
        )
    }
}

data class RequestWorkTypeState(
    val workTypeIdOrgNameLookup: Map<Long, String> = emptyMap(),
    val orgIdContactListLookup: Map<Long, List<String>> = emptyMap(),
)