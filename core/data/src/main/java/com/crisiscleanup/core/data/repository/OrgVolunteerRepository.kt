package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.IncidentOrganizationInviteInfo
import com.crisiscleanup.core.model.data.InvitationRequest
import com.crisiscleanup.core.model.data.JoinOrgInvite
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import javax.inject.Inject

interface OrgVolunteerRepository {
    suspend fun requestInvitation(invite: InvitationRequest): InvitationRequestResult?
    suspend fun getInvitationInfo(inviteCode: String): OrgUserInviteInfo?
    suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo?
    suspend fun acceptInvitation(invite: CodeInviteAccept): JoinOrgResult

    suspend fun getOrganizationInvite(organizationId: Long, inviterUserId: Long): JoinOrgInvite
    suspend fun acceptPersistentInvitation(invite: CodeInviteAccept): JoinOrgResult

    suspend fun inviteToOrganization(emailAddress: String, organizationId: Long?): Boolean
    suspend fun createOrganization(
        referer: String,
        invite: IncidentOrganizationInviteInfo,
    ): Boolean
}

class CrisisCleanupOrgVolunteerRepository @Inject constructor() : OrgVolunteerRepository {
    override suspend fun requestInvitation(invite: InvitationRequest): InvitationRequestResult? {
        TODO("Not yet implemented")
    }

    override suspend fun getInvitationInfo(inviteCode: String): OrgUserInviteInfo? {
        TODO("Not yet implemented")
    }

    override suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo? {
        TODO("Not yet implemented")
    }

    override suspend fun acceptInvitation(invite: CodeInviteAccept): JoinOrgResult {
        TODO("Not yet implemented")
    }

    override suspend fun getOrganizationInvite(
        organizationId: Long,
        inviterUserId: Long,
    ): JoinOrgInvite {
        TODO("Not yet implemented")
    }

    override suspend fun acceptPersistentInvitation(invite: CodeInviteAccept): JoinOrgResult {
        TODO("Not yet implemented")
    }

    override suspend fun inviteToOrganization(
        emailAddress: String,
        organizationId: Long?,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createOrganization(
        referer: String,
        invite: IncidentOrganizationInviteInfo,
    ): Boolean {
        TODO("Not yet implemented")
    }
}

data class InvitationRequestResult(
    val organizationName: String,
    val organizationRecipient: String,
)
