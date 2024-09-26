package com.crisiscleanup.core.network

import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.ExistingUserCodeInviteAccept
import com.crisiscleanup.core.model.data.IncidentOrganizationInviteInfo
import com.crisiscleanup.core.model.data.InvitationRequest
import com.crisiscleanup.core.model.data.InvitationRequestResult
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.OrgInviteResult
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import com.crisiscleanup.core.network.model.NetworkPersistentInvitation

interface CrisisCleanupRegisterApi {
    suspend fun registerOrgVolunteer(invite: InvitationRequest): InvitationRequestResult?

    suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo?

    suspend fun getInvitationInfo(inviteCode: String): OrgUserInviteInfo?

    suspend fun acceptOrgInvitation(invite: CodeInviteAccept): JoinOrgResult

    suspend fun createPersistentInvitation(
        userId: Long,
        targetId: Long,
        targetType: String = "organization",
    ): NetworkPersistentInvitation

    suspend fun acceptPersistentInvitation(invite: CodeInviteAccept): JoinOrgResult
    suspend fun acceptPersistentInvitation(invite: ExistingUserCodeInviteAccept): JoinOrgResult

    suspend fun inviteToOrganization(emailAddress: String, organizationId: Long?): OrgInviteResult

    suspend fun registerOrganization(
        referer: String,
        invite: IncidentOrganizationInviteInfo,
    ): Boolean
}
