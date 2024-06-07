package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.isPast
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.InvitationRequest
import com.crisiscleanup.core.model.data.InvitationRequestResult
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.LanguageIdName
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import com.crisiscleanup.feature.authentication.model.UserInfoInputData
import com.crisiscleanup.feature.authentication.navigation.RequestOrgAccessArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class RequestOrgAccessViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val languageRepository: LanguageTranslationsRepository,
    private val orgVolunteerRepository: OrgVolunteerRepository,
    private val inputValidator: InputValidator,
    private val externalEventBus: ExternalEventBus,
    private val translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Onboarding) private val logger: AppLogger,
) : ViewModel() {
    private val editorArgs = RequestOrgAccessArgs(savedStateHandle)

    private val invitationCode = editorArgs.inviteCode ?: ""
    val showEmailInput = editorArgs.showEmailInput ?: false

    private val isFetchingInviteInfo =
        MutableStateFlow(!showEmailInput && invitationCode.isNotBlank())

    val userInfo = UserInfoInputData()

    val inviteDisplay = MutableStateFlow<InviteDisplayInfo?>(null)

    val inviteInfoErrorMessage = MutableStateFlow("")

    private val isPullingLanguageOptions = MutableStateFlow(false)
    val languageOptions = MutableStateFlow<List<LanguageIdName>>(emptyList())

    private val isRequestingInvite = MutableStateFlow(false)

    private val requestedOrg = MutableStateFlow<InvitationRequestResult?>(null)

    val isInviteRequested = MutableStateFlow(false)
    var requestSentTitle by mutableStateOf("")
    var requestSentText by mutableStateOf("")

    val screenTitle = requestedOrg
        .map {
            val key = if (it == null) "actions.sign_up" else "actions.request_access"
            translator(key)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    val isLoading = combine(
        isPullingLanguageOptions,
        isFetchingInviteInfo,
        isRequestingInvite,
        ::Triple,
    )
        .map { (b0, b1, b2) -> b0 || b1 || b2 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    var emailAddress by mutableStateOf("")
    var emailAddressError by mutableStateOf("")

    val isEditable = combine(
        isFetchingInviteInfo,
        isRequestingInvite,
        ::Pair,
    )
        .map { (b0, b1) -> !(b0 || b1) }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        requestedOrg
            .onEach { result ->
                result?.let {
                    if (it.isNewAccountRequest) {
                        if (showEmailInput) {
                            requestSentTitle = translator("requestAccess.request_sent")
                            requestSentText = translator("requestAccess.request_sent_to_org")
                                .replace("{organization}", result.organizationName)
                                .replace("{requested_to}", result.organizationRecipient)
                        } else {
                            requestSentTitle = translator("info.success")
                            requestSentText =
                                translator("invitationSignup.success_accept_invitation")
                        }
                    } else {
                        inviteInfoErrorMessage.value =
                            translator("requestAccess.already_in_org_error")
                    }
                }

                isInviteRequested.value = result?.isNewAccountRequest == true
            }
            .launchIn(viewModelScope)

        languageOptions
            .onEach {
                if (it.isNotEmpty() && userInfo.language.name.isBlank()) {
                    userInfo.language = languageRepository.getRecommendedLanguage(it)
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch(ioDispatcher) {
            if (!isPullingLanguageOptions.value) {
                isPullingLanguageOptions.value = true
                try {
                    languageOptions.value = languageRepository.getLanguageOptions()
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    isPullingLanguageOptions.value = false
                }
            }
        }

        if (isFetchingInviteInfo.value) {
            viewModelScope.launch(ioDispatcher) {
                var errorMessage = ""
                try {
                    val inviteInfo = orgVolunteerRepository.getInvitationInfo(invitationCode)
                    if (inviteInfo == null) {
                        errorMessage = translator("requestAccess.invite_error")
                    } else {
                        if (inviteInfo.isExpiredInvite) {
                            errorMessage = translator("requestAccess.invite_expired_try_again")
                        } else {
                            inviteDisplay.value = InviteDisplayInfo(
                                inviteInfo,
                                translator("requestAccess.invited_you_to_join_org")
                                    .replace("{email}", inviteInfo.invitedEmail)
                                    .replace("{organization}", inviteInfo.orgName),
                            )

                            if (userInfo.emailAddress.isBlank()) {
                                userInfo.emailAddress =
                                    inviteDisplay.value?.inviteInfo?.invitedEmail ?: ""
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = translator("requestAccess.invite_error")
                    logger.logException(e)
                } finally {
                    isFetchingInviteInfo.value = false
                }

                inviteInfoErrorMessage.value = errorMessage
            }
        }
    }

    fun clearInviteCode() {
        externalEventBus.onOrgUserInvite("")
    }

    fun clearErrors() {
        emailAddressError = ""
        userInfo.clearErrors()
    }

    fun onVolunteerWithOrg() {
        clearErrors()

        if (showEmailInput &&
            !inputValidator.validateEmailAddress(emailAddress)
        ) {
            emailAddressError = translator("invitationSignup.email_error")
        }

        userInfo.validateInput(inputValidator, translator)

        if (emailAddressError.isNotBlank() ||
            userInfo.hasError ||
            // TODO Default to US English when blank rather than silently exiting
            userInfo.language.name.isBlank()
        ) {
            return
        }

        if (isRequestingInvite.value) {
            return
        }
        isRequestingInvite.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                if (showEmailInput) {
                    requestedOrg.value = orgVolunteerRepository.requestInvitation(
                        InvitationRequest(
                            firstName = userInfo.firstName,
                            lastName = userInfo.lastName,
                            emailAddress = userInfo.emailAddress,
                            title = userInfo.title,
                            password = userInfo.password,
                            mobile = userInfo.phone,
                            languageId = userInfo.language.id,
                            inviterEmailAddress = emailAddress,
                        ),
                    )
                } else if (invitationCode.isNotBlank()) {
                    // TODO Test
                    val inviteResult = orgVolunteerRepository.acceptInvitation(
                        CodeInviteAccept(
                            firstName = userInfo.firstName,
                            lastName = userInfo.lastName,
                            emailAddress = userInfo.emailAddress,
                            title = userInfo.title,
                            password = userInfo.password,
                            mobile = userInfo.phone,
                            languageId = userInfo.language.id,
                            invitationCode = invitationCode,
                        ),
                    )
                    if (inviteResult == JoinOrgResult.Success) {
                        val inviteInfo = inviteDisplay.value?.inviteInfo
                        val orgName = inviteInfo?.orgName ?: ""
                        requestedOrg.value = InvitationRequestResult(
                            organizationName = orgName,
                            organizationRecipient = inviteInfo?.inviterEmail ?: "",
                            isNewAccountRequest = orgName.isNotBlank(),
                        )
                    } else {
                        var errorMessageTranslateKey = "requestAccess.join_org_error"
                        if (invitationCode.isBlank() &&
                            inviteDisplay.value?.inviteInfo?.expiration?.isPast == true
                        ) {
                            errorMessageTranslateKey = "requestAccess.invite_expired_try_again"
                        }
                        inviteInfoErrorMessage.value = translator(errorMessageTranslateKey)
                    }
                }
            } catch (e: Exception) {
                inviteInfoErrorMessage.value =
                    translator("requestAccess.request_access_not_working")
                logger.logException(e)
            } finally {
                isRequestingInvite.value = false
            }
        }
    }
}

data class InviteDisplayInfo(
    val inviteInfo: OrgUserInviteInfo,
    val inviteMessage: String,
) {
    val avatarUrl: URL?
        get() = inviteInfo.inviterAvatarUrl
    val displayName: String
        get() = inviteInfo.displayName
}
