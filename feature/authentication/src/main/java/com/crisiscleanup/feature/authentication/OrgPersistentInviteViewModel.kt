package com.crisiscleanup.feature.authentication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.event.ExternalEventBus
import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.isPast
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.ExpiredNetworkOrgInvite
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.LanguageIdName
import com.crisiscleanup.feature.authentication.model.UserInfoInputData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrgPersistentInviteViewModel @Inject constructor(
    private val externalEventBus: ExternalEventBus,
    private val languageRepository: LanguageTranslationsRepository,
    private val orgVolunteerRepository: OrgVolunteerRepository,
    private val inputValidator: InputValidator,
    private val translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Onboarding) private val logger: AppLogger,
) : ViewModel() {

    private var invite = UserPersistentInvite(0, "")

    val userInfo = UserInfoInputData()

    val inviteDisplay = MutableStateFlow<InviteDisplayInfo?>(null)

    private val isPullingLanguageOptions = MutableStateFlow(false)
    val languageOptions = MutableStateFlow<List<LanguageIdName>>(emptyList())

    val isJoiningOrg = MutableStateFlow(false)

    val isInviteAccepted = MutableStateFlow(false)
    var acceptedTitle by mutableStateOf("")

    val inviteFailMessage = MutableStateFlow("")

    val isLoading = combine(
        inviteDisplay,
        isJoiningOrg,
        ::Pair,
    )
        .map { (invite, b1) -> invite == null || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val isEditable = combine(
        isJoiningOrg,
        isInviteAccepted,
        ::Pair,
    )
        .map { (b0, b1) -> !(b0 || b1) }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        externalEventBus.orgPersistentInvites.onEach {
            if (it.isValidInvite) {
                invite = it
                clearOrgInvite()
                queryInviteInfo(invite)
            }
        }
            .flowOn(ioDispatcher)
            .launchIn(viewModelScope)

        languageOptions
            .onEach {
                if (it.isNotEmpty() && userInfo.language.name.isBlank()) {
                    userInfo.language = languageRepository.getRecommendedLanguage(it)
                }
            }
            .flowOn(ioDispatcher)
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
    }

    private fun clearOrgInvite() {
        externalEventBus.onOrgPersistentInvite(0, "")
    }

    private fun queryInviteInfo(persistentInvite: UserPersistentInvite) =
        viewModelScope.launch(ioDispatcher) {
            if (persistentInvite.isValidInvite && !isJoiningOrg.value) {
                isJoiningOrg.value = true
                try {
                    val inviteInfo = orgVolunteerRepository.getInvitationInfo(persistentInvite)
                        ?: ExpiredNetworkOrgInvite
                    val displayInfo = InviteDisplayInfo(
                        inviteInfo,
                        translator("persistentInvitations.is_inviting_to_join_org")
                            .replace("{organization}", inviteInfo.orgName),
                    )
                    inviteDisplay.value = displayInfo
                } catch (e: Exception) {
                    logger.logException(e)
                    inviteFailMessage.value =
                        translator("persistentInvitations.invitation_error_try_again_later")
                } finally {
                    isJoiningOrg.value = false
                }
            }
        }

    private fun clearErrors() {
        inviteFailMessage.value = ""
        userInfo.clearErrors()
    }

    fun onVolunteerWithOrg() {
        clearErrors()

        userInfo.validateInput(inputValidator, translator)

        if (userInfo.hasError ||
            // TODO Default to US English when blank rather than silently exiting
            userInfo.language.name.isBlank()
        ) {
            logger.logDebug("Error of sorts ${userInfo.hasError} ${userInfo.language.name}")
            return
        }

        if (isJoiningOrg.value) {
            return
        }
        isJoiningOrg.value = true
        viewModelScope.launch(ioDispatcher) {
            try {
                val joinResult = orgVolunteerRepository.acceptPersistentInvitation(
                    CodeInviteAccept(
                        firstName = userInfo.firstName,
                        lastName = userInfo.lastName,
                        emailAddress = userInfo.emailAddress,
                        title = userInfo.title,
                        password = userInfo.password,
                        mobile = userInfo.phone,
                        languageId = userInfo.language.id,
                        invitationCode = invite.inviteToken,
                    ),
                )

                if (joinResult == JoinOrgResult.Success) {
                    acceptedTitle = translator("persistentInvitations.account_created")
                    isInviteAccepted.value = true

                    // TODO: Back to sign in with email?
                    //       Or retrieve and set tokens?
                } else {
                    var errorMessageTranslateKey = "persistentInvitations.join_org_error"
                    when (joinResult) {
                        JoinOrgResult.Redundant ->
                            errorMessageTranslateKey =
                                "persistentInvitations.already_in_org_error"

                        else -> {
                            if (inviteDisplay.value?.inviteInfo?.expiration?.isPast == true) {
                                errorMessageTranslateKey =
                                    "persistentInvitations.invite_expired_try_again"
                            }
                        }
                    }
                    inviteFailMessage.value = translator(errorMessageTranslateKey)
                }
            } catch (e: Exception) {
                logger.logException(e)
                inviteFailMessage.value =
                    translator("persistentInvitations.invitation_error_try_again_later")
            } finally {
                isJoiningOrg.value = false
            }
        }
    }
}
