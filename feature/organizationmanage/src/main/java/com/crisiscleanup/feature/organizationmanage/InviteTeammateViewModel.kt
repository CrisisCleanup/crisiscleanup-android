package com.crisiscleanup.feature.organizationmanage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.AppSettingsProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.QrCodeGenerator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Onboarding
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.IncidentSelectManager
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import com.crisiscleanup.core.data.repository.OrganizationsRepository
import com.crisiscleanup.core.domain.IncidentsData
import com.crisiscleanup.core.domain.LoadSelectIncidents
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentOrganizationInviteInfo
import com.crisiscleanup.core.model.data.JoinOrgTeamInvite
import com.crisiscleanup.core.model.data.OrgInviteResult
import com.crisiscleanup.core.model.data.OrganizationIdName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class InviteTeammateViewModel @Inject constructor(
    settingsProvider: AppSettingsProvider,
    accountDataRepository: AccountDataRepository,
    incidentsRepository: IncidentsRepository,
    incidentSelector: IncidentSelector,
    appPreferencesRepository: LocalAppPreferencesRepository,
    organizationsRepository: OrganizationsRepository,
    private val orgVolunteerRepository: OrgVolunteerRepository,
    private val inputValidator: InputValidator,
    qrCodeGenerator: QrCodeGenerator,
    incidentSelectManager: IncidentSelectManager,
    private val syncPuller: SyncPuller,
    private val translator: KeyResourceTranslator,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(Onboarding) private val logger: AppLogger,
) : ViewModel() {
    private val inviteUrl = "${settingsProvider.baseUrl}/mobile_app_user_invite"

    private val isValidatingAccount = MutableStateFlow(false)

    private val accountData = accountDataRepository.accountData
        .shareIn(
            scope = viewModelScope,
            replay = 1,
            started = SharingStarted.WhileSubscribed(),
        )
    val hasValidTokens = accountData.map { it.areTokensValid }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val loadSelectIncidents = LoadSelectIncidents(
        incidentsRepository = incidentsRepository,
        accountDataRepository = accountDataRepository,
        incidentSelector = incidentSelector,
        appPreferencesRepository = appPreferencesRepository,
        coroutineScope = viewModelScope,
    )
    private val incidentsData = loadSelectIncidents.data

    val inviteToAnotherOrg = MutableStateFlow(false)
    private val affiliateOrganizationIds = MutableStateFlow<Set<Long>?>(null)
    private val selectedOtherOrg = MutableStateFlow(OrganizationIdName(0, ""))
    val organizationNameQuery = MutableStateFlow("")
    private val isSearchingLocalOrganizations = MutableStateFlow(false)
    private val isSearchingNetworkOrganizations = MutableStateFlow(false)
    val isSearchingOrganizations = combine(
        isSearchingLocalOrganizations,
        isSearchingNetworkOrganizations,
        ::Pair,
    )
        .map { it.first || it.second }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val otherOrgQuery = combine(
        inviteToAnotherOrg,
        organizationNameQuery,
        ::Pair,
    )
        .map { (inviteToAnother, q) -> if (inviteToAnother) q else "" }
    val inviteOrgState = combine(
        inviteToAnotherOrg,
        selectedOtherOrg,
        otherOrgQuery,
        affiliateOrganizationIds,
    ) { inviteToAnother, selectedOther, otherQ, affiliateIds ->
        Pair(
            Pair(inviteToAnother, selectedOther),
            Pair(otherQ, affiliateIds),
        )
    }
        .filter { (_, b) ->
            val affiliates = b.second
            affiliates != null
        }
        .map { (a, b) ->
            val (inviteToAnother, selected) = a
            val (q, affiliates) = b
            var isNew = false
            var isAffiliate = false
            var isNonAffiliate = false

            if (inviteToAnother &&
                q.isNotBlank()
            ) {
                if (selected.id > 0 &&
                    q.trim() == selected.name.trim()
                ) {
                    if (affiliates!!.contains(selected.id)) {
                        isAffiliate = true
                    } else {
                        isNonAffiliate = true
                    }
                }

                isNew = !(isAffiliate || isNonAffiliate)
            }

            InviteOrgState(
                own = !inviteToAnother,
                affiliate = isAffiliate,
                nonAffiliate = isNonAffiliate,
                new = isNew,
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = InviteOrgState(
                own = false,
                affiliate = false,
                nonAffiliate = false,
                new = false,
            ),
            started = SharingStarted.WhileSubscribed(),
        )

    private val organizationQuery = organizationNameQuery
        .throttleLatest(300)
        .map(String::trim)
        .shareIn(
            scope = viewModelScope,
            replay = 1,
            started = SharingStarted.WhileSubscribed(),
        )

    val organizationsSearchResult = organizationQuery
        .flatMapLatest { q ->
            // TODO Indicate loading (in thread safe manner) when querying local matches
            // TODO Data layer (streamMatchingOrganizations) needs testing
            if (q.isNotBlank()) {
                organizationsRepository.streamMatchingOrganizations(q)
                    .map { OrgSearch(q, it) }
            } else {
                flowOf(EmptyOrgSearch)
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = EmptyOrgSearch,
            started = SharingStarted.WhileSubscribed(),
        )

    var inviteEmailAddresses by mutableStateOf("")
    var invitePhoneNumber by mutableStateOf("")
    var inviteFirstName by mutableStateOf("")
    var inviteLastName by mutableStateOf("")
    var emailAddressError by mutableStateOf("")
    var phoneNumberError by mutableStateOf("")
    var firstNameError by mutableStateOf("")
    var lastNameError by mutableStateOf("")
    var selectedIncidentError by mutableStateOf("")

    val incidents = MutableStateFlow(emptyList<Incident>())
    val incidentLookup = MutableStateFlow(emptyMap<Long, Incident>())
    var selectedIncidentId by mutableLongStateOf(EmptyIncident.id)

    // TODO Size QR codes relative to min screen dimension
    private val qrCodeSize = 512 + 256

    private val isCreatingMyOrgPersistentInvitation = MutableStateFlow(false)
    private val joinMyOrgInvite = MutableStateFlow<JoinOrgTeamInvite?>(null)
    private val isGeneratingMyOrgQrCode = MutableStateFlow(false)
    val myOrgInviteQrCode = combine(
        accountData,
        joinMyOrgInvite,
        ::Pair,
    )
        .filter { (_, invite) ->
            invite != null
        }
        .map { (account, invite) ->
            // TODO Atomic state updates
            isGeneratingMyOrgQrCode.value = true
            try {
                if (invite?.isExpired == false) {
                    val inviteUrl = makeInviteUrl(account.id, invite)
                    return@map qrCodeGenerator.generate(inviteUrl, qrCodeSize)?.asImageBitmap()
                }
            } finally {
                isGeneratingMyOrgQrCode.value = false
            }

            null
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    // TODO Test affiliate org features when supported
    private val generatingAffiliateOrgQrCode = MutableStateFlow(0L)
    private val affiliateOrgInviteQrCode = combine(
        accountData,
        selectedOtherOrg,
        affiliateOrganizationIds,
        ::Triple,
    )
        .filter { (accountData, otherOrg, affiliates) ->
            accountData.hasAuthenticated &&
                otherOrg.id > 0 &&
                affiliates?.contains(otherOrg.id) == true
        }
        .map { (account, otherOrgIdName, _) ->
            withContext(ioDispatcher) {
                // TODO Use other org network ID not local ID
                val otherNetworkOrganizationId = otherOrgIdName.id
                generatingAffiliateOrgQrCode.value = otherNetworkOrganizationId
                try {
                    val userId = account.id
                    val invite = orgVolunteerRepository.getOrganizationInvite(
                        networkOrganizationId = otherNetworkOrganizationId,
                        inviterUserId = userId,
                    )

                    ensureActive()

                    val inviteUrl = makeInviteUrl(account.id, invite)
                    val qrCode = qrCodeGenerator.generate(inviteUrl, qrCodeSize)?.asImageBitmap()

                    ensureActive()

                    OrgQrCode(otherNetworkOrganizationId, qrCode, invite.expiresAt)
                } finally {
                    // TODO Atomic update
                    if (generatingAffiliateOrgQrCode.value == otherNetworkOrganizationId) {
                        generatingAffiliateOrgQrCode.value = 0
                    }
                }
            }
        }

    val affiliateOrgQrCode = combine(
        inviteToAnotherOrg,
        inviteOrgState,
        selectedOtherOrg,
        affiliateOrgInviteQrCode,
    ) { inviteToAnother, inviteOrg, selectedOther, invite ->
        Pair(
            Pair(inviteToAnother, inviteOrg),
            Pair(selectedOther, invite),
        )
    }
        .map { (a, b) ->
            val (inviteToAnother, inviteOrg) = a
            val (selectedOther, invite) = b
            if (inviteToAnother &&
                inviteOrg.ownOrAffiliate &&
                invite.orgId > 0 &&
                selectedOther.id == invite.orgId
            ) {
                invite.qrCode
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            initialValue = null,
            started = SharingStarted.WhileSubscribed(),
        )

    private val isGeneratingAffiliateQrCode = combine(
        generatingAffiliateOrgQrCode,
        selectedOtherOrg,
        ::Pair,
    )
        .map { (generatingOrgId, selectedOrg) ->
            generatingOrgId > 0L && generatingOrgId == selectedOrg.id
        }
        .shareIn(
            scope = viewModelScope,
            replay = 1,
            started = SharingStarted.WhileSubscribed(),
        )

    val isGeneratingQrCode = combine(
        isCreatingMyOrgPersistentInvitation,
        isGeneratingMyOrgQrCode,
        isGeneratingAffiliateQrCode,
        ::Triple,
    )
        .map { it.first || it.second || it.third }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val isSendingInvite = MutableStateFlow(false)
    val isInviteSent = MutableStateFlow(false)
    var inviteSentTitle by mutableStateOf("")
    var inviteSentText by mutableStateOf("")

    val sendInviteErrorMessage = MutableStateFlow("")

    val isLoadingIncidents = incidentsRepository.isLoading
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )
    val isLoading = combine(
        isValidatingAccount,
        affiliateOrganizationIds,
        ::Pair,
    )
        .map { (b0, affiliateIds) ->
            b0 || affiliateIds == null
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )
    val isEditable = isSendingInvite
        .map(Boolean::not)
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val myOrgInviteOptionText = accountData.map {
        translator("inviteTeammates.part_of_my_org")
            .replace("{my_organization}", it.org.name)
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val anotherOrgInviteOptionText = translator("inviteTeammates.from_another_org")

    val scanQrCodeText = combine(
        accountData,
        inviteToAnotherOrg,
        ::Pair,
    )
        .filter { (account, _) -> account.id > 0 }
        .map { (account, inviteToOther) ->
            if (inviteToOther) {
                translator("inviteTeammates.invite_via_qr")
            } else {
                translator("inviteTeammates.scan_qr_code_to_invite")
                    .replace("{organization}", account.org.name)
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        incidentsData
            .filter { it is IncidentsData.Incidents }
            .onEach {
                val incidents = (it as IncidentsData.Incidents).incidents
                this@InviteTeammateViewModel.incidents.value = incidents
                incidentLookup.value = incidents.associateBy(Incident::id)
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        incidentSelectManager.incidentId
            .onEach {
                if (selectedIncidentId <= 0) {
                    selectedIncidentId = it
                }
            }
            .launchIn(viewModelScope)

        hasValidTokens
            .onEach {
                if (it) {
                    isValidatingAccount.value = false
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            val orgId = accountData.first().org.id
            // TODO Handle sync fail accordingly
            organizationsRepository.syncOrganization(orgId, force = true, updateLocations = false)
            affiliateOrganizationIds.value =
                organizationsRepository.getOrganizationAffiliateIds(orgId, false)
        }

        organizationQuery
            .filter { it.length > 1 }
            .onEach { q ->
                // TODO Review loading pattern and fix as necessary
                isSearchingNetworkOrganizations.value = true
                try {
                    organizationsRepository.searchOrganizations(q)
                } finally {
                    isSearchingNetworkOrganizations.value = false
                }
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        organizationNameQuery
            .onEach { q ->
                if (q.isNotBlank()) {
                    sendInviteErrorMessage.value = ""
                }
            }
            .launchIn(viewModelScope)

        accountData
            .filter { it.hasAuthenticated }
            .map { data ->
                isCreatingMyOrgPersistentInvitation.value = true
                try {
                    val orgId = data.org.id

                    joinMyOrgInvite.value?.let { invite ->
                        if (invite.targetId == orgId &&
                            invite.expiresAt > Clock.System.now().plus(5.minutes)
                        ) {
                            return@map invite
                        }
                    }

                    orgVolunteerRepository.getOrganizationInvite(orgId, data.id)
                } finally {
                    isCreatingMyOrgPersistentInvitation.value = false
                }
            }
            .onEach {
                joinMyOrgInvite.value = it
            }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        inviteOrgState
            .throttleLatest(300)
            .onEach {
                clearErrors()
            }
            .launchIn(viewModelScope)
    }

    private fun clearErrors() {
        emailAddressError = ""
        phoneNumberError = ""
        firstNameError = ""
        lastNameError = ""
        selectedIncidentError = ""

        sendInviteErrorMessage.value = ""
    }

    private fun makeInviteUrl(userId: Long, invite: JoinOrgTeamInvite): String {
        return "$inviteUrl?org-id=${invite.targetId}&user-id=$userId&invite-token=${invite.token}"
    }

    fun refreshIncidents() {
        syncPuller.appPull(true, cancelOngoing = false)
    }

    fun onSelectOrganization(organization: OrganizationIdName) {
        selectedOtherOrg.value = organization
        organizationNameQuery.value = organization.name
    }

    private fun validateSendEmailAddresses(): List<String> {
        val emailAddresses = inviteEmailAddresses.split(",")
            .map(String::trim)
            .filter(String::isNotBlank)
        val errorMessage = if (emailAddresses.isEmpty()) {
            translator("inviteTeammates.enter_invited_emails")
        } else {
            val invalidEmailAddresses = emailAddresses.map { s ->
                if (inputValidator.validateEmailAddress(s)) {
                    ""
                } else {
                    translator("inviteTeammates.invalid_email_error")
                        .replace("{email}", s)
                }
            }
            invalidEmailAddresses.filter(String::isNotBlank)
                .joinToString("\n")
        }

        if (errorMessage.isNotBlank()) {
            emailAddressError = errorMessage
            return emptyList()
        }

        return emailAddresses
    }

    fun onOrgQueryClose() {
        var matchingOrg = OrganizationIdName(0, "")
        val q = organizationNameQuery.value.trim()
        val orgQueryLower = q.lowercase()
        // TODO Optimize matching if result set is computationally large
        for (result in organizationsSearchResult.value.organizations) {
            if (result.name.trim().lowercase() == orgQueryLower) {
                matchingOrg = result
                break
            }
        }
        val selectedOrgId = selectedOtherOrg.value.id
        if ((selectedOrgId == 0L || selectedOrgId != matchingOrg.id) &&
            matchingOrg.id == 0L
        ) {
            matchingOrg = OrganizationIdName(0, q)
        }
        selectedOtherOrg.value = matchingOrg
        organizationNameQuery.value = matchingOrg.name
    }

    private suspend fun inviteToOrgOrAffiliate(
        emailAddresses: List<String>,
        organizationId: Long? = null,
    ): List<InviteResult> {
        val inviteResults = mutableListOf<InviteResult>()
        for (emailAddress in emailAddresses) {
            val result = orgVolunteerRepository.inviteToOrganization(emailAddress, organizationId)
            inviteResults.add(InviteResult(emailAddress, result))
        }
        return inviteResults
    }

    private fun onInviteSent(title: String, text: String) {
        inviteSentTitle = title
        inviteSentText = text
        isInviteSent.value = true
    }

    private fun onInviteSentToOrgOrAffiliate(emailAddresses: List<String>) {
        onInviteSent(
            title = translator("inviteTeammates.invitations_sent"),
            text = translator(emailAddresses.joinToString("\n")),
        )
    }

    fun onSendInvites() {
        if (isSendingInvite.value) {
            return
        }
        isSendingInvite.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                sendInvites()
            } catch (e: Exception) {
                sendInviteErrorMessage.value =
                    translator("inviteTeammates.invite_error")
                logger.logException(e)
            } finally {
                isSendingInvite.value = false
            }
        }
    }

    private suspend fun sendInvites() {
        emailAddressError = ""
        phoneNumberError = ""
        firstNameError = ""
        lastNameError = ""
        selectedIncidentError = ""

        sendInviteErrorMessage.value = ""

        val emailAddress = accountData.first().emailAddress
        val myEmailAddressLower = emailAddress.trim().lowercase()
        val emailAddresses = validateSendEmailAddresses()
            .filter { s -> s.lowercase() != myEmailAddressLower }
        if (emailAddresses.isEmpty()) {
            return
        }

        val inviteState = inviteOrgState.first()
        if (inviteState.new) {
            if (emailAddresses.size > 1) {
                emailAddressError = translator("registerOrg.only_one_email_allowed")
                return
            }

            if (invitePhoneNumber.isBlank()) {
                phoneNumberError = translator("registerOrg.phone_required")
                return
            }

            if (inviteFirstName.isBlank()) {
                firstNameError = translator("registerOrg.first_name_required")
                return
            }

            if (inviteLastName.isBlank()) {
                lastNameError = translator("registerOrg.last_name_required")
                return
            }

            if (selectedIncidentId == EmptyIncident.id) {
                selectedIncidentError = translator("registerOrg.select_incident")
                return
            }
        }

        val q = organizationNameQuery.value
        if (inviteToAnotherOrg.value) {
            if (selectedOtherOrg.value.id > 0) {
                if (selectedOtherOrg.value.name != q) {
                    sendInviteErrorMessage.value = translator("registerOrg.search_and_select_org")
                    return
                }
            } else {
                if (q.trim().isBlank()) {
                    sendInviteErrorMessage.value =
                        translator("registerOrg.search_and_select_org_blank")
                    return
                }
            }
        }

        var inviteResults = emptyList<InviteResult>()
        var isInviteSuccessful = false
        if (inviteToAnotherOrg.value) {
            if (inviteState.new) {
                val organizationName = q.trim()
                val emailContact = emailAddresses[0]
                val isRegisterNewOrganization = orgVolunteerRepository.createOrganization(
                    referer = emailAddress,
                    invite = IncidentOrganizationInviteInfo(
                        incidentId = selectedIncidentId,
                        organizationName = organizationName,
                        emailAddress = emailContact,
                        mobile = invitePhoneNumber,
                        firstName = inviteFirstName,
                        lastName = inviteLastName,
                    ),
                )

                if (isRegisterNewOrganization) {
                    onInviteSent(
                        title = translator("registerOrg.you_have_registered_org")
                            .replace("{organization}", organizationName),
                        text = translator("registerOrg.we_will_finalize_registration")
                            .replace("{email}", emailContact),
                    )

                    isInviteSuccessful = true
                }
            } else if (inviteState.affiliate) {
                inviteResults = inviteToOrgOrAffiliate(emailAddresses, selectedOtherOrg.value.id)
                isInviteSuccessful =
                    inviteResults.any { it.inviteResult == OrgInviteResult.Invited }
            } else if (inviteState.nonAffiliate) {
                // TODO Finish when API supports a corresponding endpoint
            }
        } else {
            inviteResults = inviteToOrgOrAffiliate(emailAddresses)
            isInviteSuccessful = inviteResults.any { it.inviteResult == OrgInviteResult.Invited }
        }

        val invited = inviteResults
            .filter { it.inviteResult == OrgInviteResult.Invited }
            .map(InviteResult::emailAddress)
        if (invited.isNotEmpty()) {
            onInviteSentToOrgOrAffiliate(invited)
        }

        if (!isInviteSuccessful) {
            val uninvited = inviteResults
                .filter { it.inviteResult != OrgInviteResult.Invited }
                .map(InviteResult::emailAddress)
            var uninvitedMessage = ""
            if (uninvited.isNotEmpty()) {
                uninvitedMessage =
                    translator("inviteTeammates.emails_not_invited_error")
                        .replace("{email_addresses}", uninvited.joinToString(", "))
            }
            sendInviteErrorMessage.value =
                uninvitedMessage.ifBlank { translator("inviteTeammates.invite_error") }
        }
    }
}

data class OrgSearch(
    val q: String,
    val organizations: List<OrganizationIdName>,
)

private val EmptyOrgSearch = OrgSearch("", emptyList())

data class OrgQrCode(
    val orgId: Long,
    val qrCode: ImageBitmap?,
    val qrCodeExpiration: Instant,
)

data class InviteOrgState(
    val own: Boolean,
    val affiliate: Boolean,
    val nonAffiliate: Boolean,
    val new: Boolean,
    val ownOrAffiliate: Boolean = own || affiliate,
)

private data class InviteResult(
    val emailAddress: String,
    val inviteResult: OrgInviteResult,
)
