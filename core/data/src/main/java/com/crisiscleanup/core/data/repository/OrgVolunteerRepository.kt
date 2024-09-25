package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers.Onboarding
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.IncidentOrganizationInviteInfo
import com.crisiscleanup.core.model.data.InvitationRequest
import com.crisiscleanup.core.model.data.InvitationRequestResult
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.JoinOrgTeamInvite
import com.crisiscleanup.core.model.data.OrgInviteResult
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import com.crisiscleanup.core.network.CrisisCleanupRegisterApi
import kotlinx.datetime.Instant
import javax.inject.Inject

interface OrgVolunteerRepository {
    suspend fun requestInvitation(invite: InvitationRequest): InvitationRequestResult?
    suspend fun getInvitationInfo(inviteCode: String): OrgUserInviteInfo?
    suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo?
    suspend fun acceptInvitation(invite: CodeInviteAccept): JoinOrgResult

    suspend fun getOrganizationInvite(organizationId: Long, inviterUserId: Long): JoinOrgTeamInvite
    suspend fun getTeamInvite(teamId: Long, inviterUserId: Long): JoinOrgTeamInvite
    suspend fun acceptPersistentInvitation(invite: CodeInviteAccept): JoinOrgResult

    suspend fun inviteToOrganization(emailAddress: String, organizationId: Long?): OrgInviteResult
    suspend fun createOrganization(
        referer: String,
        invite: IncidentOrganizationInviteInfo,
    ): Boolean
}

class CrisisCleanupOrgVolunteerRepository @Inject constructor(
    private val registerApi: CrisisCleanupRegisterApi,
    @Logger(Onboarding) private val logger: AppLogger,
) : OrgVolunteerRepository {
    override suspend fun requestInvitation(invite: InvitationRequest): InvitationRequestResult? {
        try {
            // TODO Handle cases where an invite was already sent to the user from the org
            return registerApi.registerOrgVolunteer(invite)
        } catch (e: Exception) {
            logger.logException(e)
        }

        return null
    }

    override suspend fun getInvitationInfo(inviteCode: String): OrgUserInviteInfo? {
        try {
            return registerApi.getInvitationInfo(inviteCode)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return null
    }

    override suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo? {
        try {
            return registerApi.getInvitationInfo(invite)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return null
    }

    override suspend fun acceptInvitation(invite: CodeInviteAccept): JoinOrgResult {
        try {
            // TODO Handle cases where an invite was already sent to the user from the org
            return registerApi.acceptOrgInvitation(invite)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return JoinOrgResult.Unknown
    }

    private suspend fun createPersistentInvite(
        targetId: Long,
        inviterUserId: Long,
        isTeamInvite: Boolean = false,
    ): JoinOrgTeamInvite {
        try {
            val inviteType = if (isTeamInvite) {
                "team"
            } else {
                "organization"
            }
            val invite = registerApi.createPersistentInvitation(
                inviterUserId,
                targetId = targetId,
                inviteType,
            )
            return JoinOrgTeamInvite(
                invite.token,
                invite.objectId,
                invite.expiresAt,
            )
        } catch (e: Exception) {
            logger.logException(e)
        }
        return JoinOrgTeamInvite("", 0, Instant.fromEpochSeconds(0))
    }

    override suspend fun getOrganizationInvite(
        organizationId: Long,
        inviterUserId: Long,
    ) = createPersistentInvite(organizationId, inviterUserId = inviterUserId)

    override suspend fun getTeamInvite(
        teamId: Long,
        inviterUserId: Long,
    ) = createPersistentInvite(teamId, inviterUserId = inviterUserId, true)

    override suspend fun acceptPersistentInvitation(invite: CodeInviteAccept): JoinOrgResult {
        try {
            return registerApi.acceptPersistentInvitation(invite)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return JoinOrgResult.Unknown
    }

    override suspend fun inviteToOrganization(
        emailAddress: String,
        organizationId: Long?,
    ): OrgInviteResult {
        try {
            return registerApi.inviteToOrganization(emailAddress, organizationId)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return OrgInviteResult.Unknown
    }

    override suspend fun createOrganization(
        referer: String,
        invite: IncidentOrganizationInviteInfo,
    ): Boolean {
        try {
            return registerApi.registerOrganization(referer, invite)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return false
    }
}
