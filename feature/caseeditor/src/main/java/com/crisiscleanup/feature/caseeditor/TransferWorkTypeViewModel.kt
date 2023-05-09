package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.OfflineFirstIncidentsRepository
import com.crisiscleanup.feature.caseeditor.WorkTypeTransferType.None
import com.crisiscleanup.feature.caseeditor.WorkTypeTransferType.Release
import com.crisiscleanup.feature.caseeditor.WorkTypeTransferType.Request
import com.crisiscleanup.feature.caseeditor.model.contactList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferWorkTypeViewModel @Inject constructor(
    incidentsRepository: OfflineFirstIncidentsRepository,
    private val editableWorksiteProvider: EditableWorksiteProvider,
    private val transferWorkTypeProvider: TransferWorkTypeProvider,
    private val translator: KeyTranslator,
    private val resourceProvider: AndroidResourceProvider,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val transferType = transferWorkTypeProvider.transferType

    val isTransferable = transferType != None && transferWorkTypeProvider.workTypes.isNotEmpty()

    val screenTitle = when (transferType) {
        Release -> translate("actions.release_cases")
        Request -> translate("workTypeRequestModal.work_type_request")
        else -> ""
    }

    val isTransferred = MutableStateFlow(false)
    val isTransferring = MutableStateFlow(false)

    val transferWorkTypesState = transferWorkTypeProvider.workTypes
    val workTypesState = mutableStateMapOf<Long, Boolean>()
        .also { map -> transferWorkTypesState.forEach { map[it.key.id] = it.value } }

    var transferReason by mutableStateOf("")

    val errorMessageReason = MutableStateFlow("")
    val errorMessageWorkType = MutableStateFlow("")

    private val requestWorkTypesState = incidentsRepository.organizationLookup.map { orgLookup ->
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

    fun translate(key: String, fallback: String? = null) = translator.translate(key)
        ?: (editableWorksiteProvider.translate(key) ?: (fallback ?: key))

    fun commitTransfer(): Boolean {
        errorMessageReason.value = ""
        if (transferReason.isBlank()) {
            val reasonResId = if (transferType == Release) R.string.release_reason_is_required
            else R.string.request_reason_is_required
            errorMessageReason.value = resourceProvider.getString(reasonResId)
        }

        errorMessageWorkType.value = ""
        if (workTypesState.filter { it.value }.isEmpty()) {
            errorMessageWorkType.value =
                resourceProvider.getString(R.string.transfer_work_type_is_required)
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
            try {
                // TODO Save changes for release or request
            } finally {
                isTransferring.value = false
            }
        }
    }
}

data class RequestWorkTypeState(
    val workTypeIdOrgNameLookup: Map<Long, String> = emptyMap(),
    val orgIdContactListLookup: Map<Long, List<String>> = emptyMap(),
)