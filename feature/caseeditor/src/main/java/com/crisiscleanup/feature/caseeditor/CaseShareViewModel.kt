package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.WorkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaseShareViewModel @Inject constructor(
    editableWorksiteProvider: EditableWorksiteProvider,
    usersRepository: UsersRepository,
    organizationsRepository: OrganizationsRepository,
    accountDataRepository: AccountDataRepository,
    private val worksitesRepository: WorksitesRepository,
    networkMonitor: NetworkMonitor,
    private val inputValidator: InputValidator,
    val translator: KeyResourceTranslator,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val worksiteIn = editableWorksiteProvider.editableWorksite.value

    val isSharing = MutableStateFlow(false)
    val isShared = MutableStateFlow(false)

    private val isOnline = networkMonitor.isOnline

    var unclaimedShareReason by mutableStateOf("")
    var isEmailContactMethod by mutableStateOf(true)
    var contactErrorMessage by mutableStateOf("")
    val receiverContacts = MutableStateFlow<List<ShareContactInfo>>(emptyList())
    val receiverContactManual = MutableStateFlow("")
    val receiverContactSuggestion = MutableStateFlow("")
    var receiverMessage by mutableStateOf("")

    private val organizationId = accountDataRepository.accountData.mapLatest { it.org.id }
        .stateIn(
            scope = viewModelScope,
            initialValue = 0L,
            started = SharingStarted.WhileSubscribed(),
        )

    val hasClaimedWorkType = organizationId.map { orgId ->
        val affiliatedOrgIds = organizationsRepository.getOrganizationAffiliateIds(orgId)
        val claimedBys = worksiteIn.workTypes.mapNotNull(WorkType::orgClaim).toSet()
        val isClaimed = claimedBys.any { claimedBy ->
            affiliatedOrgIds.contains(
                claimedBy
            )
        }

        if (isClaimed) {
            showShareScreen = true
        }
        isClaimed
    }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    val isLoading = hasClaimedWorkType.map { it == null }
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )
    var showShareScreen by mutableStateOf(false)

    private val isSharable = combine(
        isOnline,
        accountDataRepository.accountData,
    ) { online, accountData ->
        online && accountData.areTokensValid
    }

    val notSharableMessage = combine(
        isOnline,
        accountDataRepository.accountData,
    ) { online, accountData ->
        if (!online) {
            translator("info.share_requires_internet")
        } else if (!accountData.areTokensValid) {
            translator("info.share_requires_login")
        } else {
            ""
        }
    }

    val isShareEnabled = combine(
        isSharable,
        isLoading,
        isSharing,
        receiverContacts,
    ) { sharable, loading, sharing, contacts ->
        sharable &&
                !loading &&
                !sharing &&
                contacts.isNotEmpty()
    }

    private val contactQuery = receiverContactSuggestion
        .map(String::trim)
        .distinctUntilChanged()
        .filter { it.length > 1 }

    val contactOptions = combine(
        organizationId,
        contactQuery,
        ::Pair,
    )
        .map { (orgId, query) ->
            val isEmailContact = isEmailContactMethod
            val contacts = usersRepository.getMatchingUsers(query, orgId).map {
                val contactValue = if (isEmailContact) it.email else it.mobile
                ShareContactInfo(
                    name = it.fullName,
                    contactValue = contactValue.trim(),
                    isEmail = isEmailContact,
                )
            }
            contacts
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        receiverContactManual
            .onEach {
                if (it.isBlank()) {
                    contactErrorMessage = ""
                }
            }
            .launchIn(viewModelScope)
    }

    fun continueToShareScreen() {
        showShareScreen = true
    }

    private fun addContact(contactInfo: ShareContactInfo) {
        val existingContacts = receiverContacts.value.map(ShareContactInfo::contactValue).toSet()
        if (!existingContacts.contains(contactInfo.contactValue)) {
            receiverContacts.value =
                receiverContacts.value.toMutableList().also { it.add(contactInfo) }
        }
    }

    fun onAddContact(contact: String) {
        contactErrorMessage = ""

        if (contact.isNotBlank()) {
            val isEmail = isEmailContactMethod
            if (isEmail) {
                if (!inputValidator.validateEmailAddress(contact)) {
                    contactErrorMessage =
                        translator("info.enter_valid_email")
                    return
                }
            } else {
                if (!inputValidator.validatePhoneNumber(contact)) {
                    contactErrorMessage =
                        translator("info.enter_valid_phone")
                    return
                }
            }

            val contactInfo = ShareContactInfo(
                "",
                contact.trim(),
                isEmail,
            )
            addContact(contactInfo)
            receiverContactManual.value = ""
        }
    }

    fun onAddContact(contact: ShareContactInfo) {
        if (contact.contactValue.isNotBlank()) {
            addContact(contact)
            receiverContactSuggestion.value = ""
        }
    }

    fun deleteContact(index: Int) {
        if (index in 0 until receiverContacts.value.size) {
            receiverContacts.value = receiverContacts.value.toMutableList().also {
                it.removeAt(index)
            }
        }
    }

    fun onShare() {
        val shareMessage = receiverMessage
        val noClaimReason = unclaimedShareReason
        val contacts = receiverContacts.value
        val emails = contacts.filter(ShareContactInfo::isEmail)
        val phoneNumbers = contacts.filterNot(ShareContactInfo::isEmail)
        if (emails.isEmpty() && phoneNumbers.isEmpty()) {
            return
        }

        if (isSharing.value) {
            return
        }
        isSharing.value = true

        viewModelScope.launch(ioDispatcher) {
            try {
                isShared.value = worksitesRepository.shareWorksite(
                    worksiteIn.id,
                    emails.map(ShareContactInfo::contactValue),
                    phoneNumbers.map(ShareContactInfo::contactValue),
                    shareMessage,
                    noClaimReason,
                )
            } finally {
                isSharing.value = false
            }
        }
    }
}

data class ShareContactInfo(
    val name: String,
    val contactValue: String,
    val isEmail: Boolean,
)