package com.crisiscleanup.feature.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.OrgVolunteerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasteOrgInviteViewModel @Inject constructor(
    private val orgVolunteerRepository: OrgVolunteerRepository,
    private val translator: KeyResourceTranslator,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Onboarding) private val logger: AppLogger,
) : ViewModel() {
    val isVerifyingCode = MutableStateFlow(false)

    val inviteCode = MutableStateFlow("")
    val inviteCodeError = MutableStateFlow("")

    private val inviteCodeRegex =
        """/invitation_token/([0-9a-f]+)$""".toRegex(RegexOption.IGNORE_CASE)

    fun onSubmitLink(link: String) {
        if (link.isBlank()) {
            inviteCodeError.value = translator("volunteerOrg.paste_invitation_link")
            return
        }

        inviteCodeError.value = ""

        viewModelScope.launch(ioDispatcher) {
            if (isVerifyingCode.value) {
                return@launch
            }
            isVerifyingCode.value = true

            try {
                var errorMessageKey = ""

                val match = inviteCodeRegex.find(link.trim())
                if (match == null) {
                    errorMessageKey = "pasteInvite.link_not_invitation"
                } else {
                    val code = match.groupValues[1]
                    val info = orgVolunteerRepository.getInvitationInfo(code)
                    if (info == null) {
                        errorMessageKey = "pasteInvite.link_invalid"
                    } else {
                        if (info.isExpiredInvite) {
                            errorMessageKey = "pasteInvite.link_expired"
                        } else {
                            inviteCode.value = code
                        }
                    }
                }

                if (errorMessageKey.isNotBlank()) {
                    inviteCodeError.value = translator(errorMessageKey)
                }
            } catch (e: Exception) {
                inviteCodeError.value =
                    translator("~~Invites are not working at the moment. Please try again later.")
                logger.logException(e)
            } finally {
                isVerifyingCode.value = false
            }
        }
    }
}
