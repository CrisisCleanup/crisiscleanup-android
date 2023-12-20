package com.crisiscleanup.core.common.event

import com.crisiscleanup.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface ExternalEventBus {
    val showResetPassword: Flow<Boolean>
    val resetPasswords: StateFlow<String>

    val showMagicLinkLogin: Flow<Boolean>
    val emailLoginCodes: Flow<String>

    val orgUserInvites: Flow<String>

    val showOrgPersistentInvite: Flow<Boolean>
    val orgPersistentInvites: Flow<UserPersistentInvite>

    fun onResetPassword(code: String)

    fun onEmailLoginLink(code: String)

    fun onOrgUserInvite(code: String)

    fun onOrgPersistentInvite(inviterUserId: Long, inviteToken: String)
    fun onOrgPersistentInvite(query: Map<String, String>): Boolean
}

@Singleton
class CrisisCleanupExternalEventBus @Inject constructor(
    @ApplicationScope private val externalScope: CoroutineScope,
) : ExternalEventBus {
    override val resetPasswords = MutableStateFlow("")
    override val showResetPassword = resetPasswords.map(String::isNotBlank)

    override val emailLoginCodes = MutableStateFlow("")
    override val showMagicLinkLogin = emailLoginCodes.map(String::isNotBlank)

    override val orgUserInvites = MutableStateFlow("")

    override val orgPersistentInvites = MutableStateFlow(UserPersistentInvite(0, ""))
    override val showOrgPersistentInvite = orgPersistentInvites.map { it.inviterUserId > 0 }

    override fun onResetPassword(code: String) {
        externalScope.launch {
            resetPasswords.value = code
        }
    }

    override fun onEmailLoginLink(code: String) {
        externalScope.launch {
            emailLoginCodes.value = code
        }
    }

    override fun onOrgUserInvite(code: String) {
        externalScope.launch {
            orgUserInvites.value = code
        }
    }

    override fun onOrgPersistentInvite(inviterUserId: Long, inviteToken: String) {
        externalScope.launch {
            orgPersistentInvites.value = UserPersistentInvite(inviterUserId, inviteToken)
        }
    }

    override fun onOrgPersistentInvite(query: Map<String, String>): Boolean {
        query["user-id"]?.let { userIdString ->
            try {
                val userId = userIdString.toLong()
                query["invite-token"]?.let { token ->
                    onOrgPersistentInvite(userId, token)
                    return true
                }
            } catch (e: Exception) {
                // Unnecessary
            }
        }
        return false
    }
}

data class UserPersistentInvite(
    val inviterUserId: Long,
    val inviteToken: String,
)
