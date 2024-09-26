package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.isPast
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.ExistingUserCodeInviteAccept
import com.crisiscleanup.core.model.data.ExpiredNetworkOrgInvite
import com.crisiscleanup.core.model.data.ExpiredNetworkTeamInvite
import com.crisiscleanup.core.model.data.IncidentOrganizationInviteInfo
import com.crisiscleanup.core.model.data.InvitationRequest
import com.crisiscleanup.core.model.data.InvitationRequestResult
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.OrgInviteResult
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import com.crisiscleanup.core.model.data.TeamInviteInfo
import com.crisiscleanup.core.network.CrisisCleanupRegisterApi
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.NetworkAcceptCodeInvite
import com.crisiscleanup.core.network.model.NetworkAcceptPersistentInvite
import com.crisiscleanup.core.network.model.NetworkAcceptedCodeInvitationRequest
import com.crisiscleanup.core.network.model.NetworkAcceptedInvitationRequest
import com.crisiscleanup.core.network.model.NetworkAcceptedPersistentInvite
import com.crisiscleanup.core.network.model.NetworkCreatePersistentInvitation
import com.crisiscleanup.core.network.model.NetworkExistingUserAcceptPersistentInvite
import com.crisiscleanup.core.network.model.NetworkInvitationInfoResult
import com.crisiscleanup.core.network.model.NetworkInvitationRequest
import com.crisiscleanup.core.network.model.NetworkOrganizationContact
import com.crisiscleanup.core.network.model.NetworkOrganizationInvite
import com.crisiscleanup.core.network.model.NetworkOrganizationInviteResult
import com.crisiscleanup.core.network.model.NetworkOrganizationRegistration
import com.crisiscleanup.core.network.model.NetworkOrganizationShort
import com.crisiscleanup.core.network.model.NetworkPersistentInvitation
import com.crisiscleanup.core.network.model.NetworkPersistentInvitationResult
import com.crisiscleanup.core.network.model.NetworkRegisterOrganizationResult
import com.crisiscleanup.core.network.model.NetworkUser
import com.crisiscleanup.core.network.model.condenseMessages
import com.crisiscleanup.core.network.model.profilePictureUrl
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private interface RegisterApi {
    @ThrowClientErrorHeader
    @POST("invitation_requests")
    suspend fun requestInvitation(
        @Body invitationRequest: NetworkInvitationRequest,
    ): NetworkAcceptedInvitationRequest

    @WrapResponseHeader("invite")
    @GET("invitations/{code}")
    suspend fun invitationInfo(
        @Path("code") inviteCode: String,
    ): NetworkInvitationInfoResult

    @WrapResponseHeader("invite")
    @GET("persistent_invitations/{code}")
    suspend fun persistentInvitationInfo(
        @Path("code") inviteCode: String,
    ): NetworkPersistentInvitationResult

    @GET("users/{user}")
    suspend fun noAuthUser(
        @Path("user") userId: Long,
    ): NetworkUser

    @GET("organizations/{organizationId}")
    suspend fun noAuthOrganization(
        @Path("organizationId") organizationId: Long,
    ): NetworkOrganizationShort

    @POST("invitations/accept")
    suspend fun acceptInvitationFromCode(
        @Body acceptInvite: NetworkAcceptCodeInvite,
    ): NetworkAcceptedCodeInvitationRequest

    @TokenAuthenticationHeader
    @WrapResponseHeader("invite")
    @POST("persistent_invitations")
    suspend fun createPersistentInvitation(
        @Body org: NetworkCreatePersistentInvitation,
    ): NetworkPersistentInvitationResult

    @ThrowClientErrorHeader
    @POST("persistent_invitations/accept")
    suspend fun acceptPersistentInvitation(
        @Body acceptInvite: NetworkAcceptPersistentInvite,
    ): NetworkAcceptedPersistentInvite

    @ThrowClientErrorHeader
    @POST("persistent_invitations/accept")
    suspend fun acceptPersistentInvitation(
        @Body acceptInvite: NetworkExistingUserAcceptPersistentInvite,
    ): NetworkAcceptedPersistentInvite

    @TokenAuthenticationHeader
    @WrapResponseHeader("invite")
    @ThrowClientErrorHeader
    @POST("invitations")
    suspend fun inviteToOrganization(
        @Body invite: NetworkOrganizationInvite,
    ): NetworkOrganizationInviteResult

    @TokenAuthenticationHeader
    @WrapResponseHeader("organization")
    @POST("organizations")
    suspend fun registerOrganization(
        @Body org: NetworkOrganizationRegistration,
    ): NetworkRegisterOrganizationResult
}

@Singleton
class RegisterApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
) : CrisisCleanupRegisterApi {
    private val networkApi = retrofit.create(RegisterApi::class.java)

    override suspend fun registerOrgVolunteer(invite: InvitationRequest): InvitationRequestResult? {
        val inviteRequest = NetworkInvitationRequest(
            firstName = invite.firstName,
            lastName = invite.lastName,
            email = invite.emailAddress,
            title = invite.title,
            password1 = invite.password,
            password2 = invite.password,
            mobile = invite.mobile,
            requestedTo = invite.inviterEmailAddress,
            primaryLanguage = invite.languageId,
        )
        try {
            val result = networkApi.requestInvitation(inviteRequest)
            return InvitationRequestResult(
                organizationName = result.requestedOrganization,
                organizationRecipient = result.requestedTo,
                isNewAccountRequest = true,
            )
        } catch (e: CrisisCleanupNetworkException) {
            if (e.errors.condenseMessages.contains("already have an account")) {
                return InvitationRequestResult("", "", false)
            }
        }

        // TODO Be explicit in result
        return null
    }

    private suspend fun getUserDetails(userId: Long): UserDetails {
        val userInfo = networkApi.noAuthUser(userId)
        val displayName = "${userInfo.firstName} ${userInfo.lastName}"
        val avatarUrl = userInfo.files.profilePictureUrl?.let { URL(it) }
        val orgName = networkApi.noAuthOrganization(userInfo.organization).name
        return UserDetails(
            displayName = displayName,
            organizationName = orgName,
            avatarUrl = avatarUrl,
        )
    }

    override suspend fun getOrgInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo? {
        networkApi.persistentInvitationInfo(invite.inviteToken).invite?.let { persistentInvite ->
            if (persistentInvite.expiresAt.isPast) {
                return ExpiredNetworkOrgInvite
            }

            val userDetails = getUserDetails(invite.inviterUserId)
            return OrgUserInviteInfo(
                displayName = userDetails.displayName,
                inviterEmail = "",
                inviterAvatarUrl = userDetails.avatarUrl,
                invitedEmail = "",
                orgName = userDetails.organizationName,
                expiration = persistentInvite.expiresAt,
                isExpiredInvite = false,
            )
        }

        return null
    }

    override suspend fun getOrgInvitationInfo(inviteCode: String): OrgUserInviteInfo? {
        networkApi.invitationInfo(inviteCode).invite?.let { invite ->
            if (invite.expiresAt.isPast) {
                return ExpiredNetworkOrgInvite
            }

            val inviter = invite.inviter
            val userDetails = getUserDetails(inviter.id)
            return OrgUserInviteInfo(
                displayName = "${inviter.firstName} ${inviter.lastName}",
                inviterEmail = inviter.email,
                inviterAvatarUrl = userDetails.avatarUrl,
                invitedEmail = invite.inviteeEmail,
                orgName = userDetails.organizationName,
                expiration = invite.expiresAt,
                isExpiredInvite = false,
            )
        }

        return null
    }

    override suspend fun getTeamInvitationInfo(invite: UserPersistentInvite): TeamInviteInfo? {
        networkApi.persistentInvitationInfo(invite.inviteToken).invite?.let { persistentInvite ->
            if (persistentInvite.expiresAt.isPast) {
                return ExpiredNetworkTeamInvite
            }

            return TeamInviteInfo(
                teamId = persistentInvite.objectId,
                expiration = persistentInvite.expiresAt,
                isExpiredInvite = false,
            )
        }

        return null
    }

    override suspend fun acceptOrgInvitation(invite: CodeInviteAccept): JoinOrgResult {
        val payload = NetworkAcceptCodeInvite(
            firstName = invite.firstName,
            lastName = invite.lastName,
            email = invite.emailAddress,
            title = invite.title,
            password = invite.password,
            mobile = invite.mobile,
            invitationToken = invite.invitationCode,
            primaryLanguage = invite.languageId,
        )
        val acceptResult = networkApi.acceptInvitationFromCode(payload)
        return if (acceptResult.status == "invitation_accepted") {
            JoinOrgResult.Success
        } else {
            JoinOrgResult.Unknown
        }
    }

    override suspend fun createPersistentInvitation(
        userId: Long,
        targetId: Long,
        targetType: String,
    ): NetworkPersistentInvitation {
        val validatedTargetType = if (targetType.lowercase() == "organization") {
            "organization"
        } else {
            "team"
        }
        val invitation = NetworkCreatePersistentInvitation(
            createdBy = userId,
            targetId = targetId,
            model = validatedTargetType,
        )
        return networkApi.createPersistentInvitation(invitation).invite
            ?: throw Exception("Persistent invite not created on the backend")
    }

    override suspend fun acceptPersistentInvitation(invite: CodeInviteAccept): JoinOrgResult {
        val payload = NetworkAcceptPersistentInvite(
            firstName = invite.firstName,
            lastName = invite.lastName,
            email = invite.emailAddress,
            title = invite.title,
            password = invite.password,
            mobile = invite.mobile,
            token = invite.invitationCode,
        )
        try {
            val response = networkApi.acceptPersistentInvitation(payload)
            return when (response.detail) {
                "You have been added to the organization." -> JoinOrgResult.Success
                "You have been added to the team." -> JoinOrgResult.Success
                "User already a member of this organization." -> JoinOrgResult.Redundant
                else -> JoinOrgResult.Unknown
            }
        } catch (e: CrisisCleanupNetworkException) {
            if (e.body.contains("User already a member of this organization.")) {
                return JoinOrgResult.Redundant
            }
        }
        return JoinOrgResult.Unknown
    }

    override suspend fun acceptPersistentInvitation(invite: ExistingUserCodeInviteAccept): JoinOrgResult {
        val payload = NetworkExistingUserAcceptPersistentInvite(
            email = invite.emailAddress,
            token = invite.invitationCode,
        )
        try {
            val response = networkApi.acceptPersistentInvitation(payload)
            return when (response.detail) {
                "User added to team." -> JoinOrgResult.Success
                "User is already a member of this team." -> JoinOrgResult.Redundant
                else -> JoinOrgResult.Unknown
            }
        } catch (e: CrisisCleanupNetworkException) {
            if (e.body.contains("User is already a member of this team.")) {
                return JoinOrgResult.Redundant
            }
        }
        return JoinOrgResult.Unknown
    }

    override suspend fun inviteToOrganization(
        emailAddress: String,
        organizationId: Long?,
    ): OrgInviteResult {
        try {
            val invite =
                networkApi.inviteToOrganization(
                    NetworkOrganizationInvite(
                        emailAddress,
                        organizationId,
                    ),
                )
            if (invite.invite?.inviteeEmail == emailAddress) {
                return OrgInviteResult.Invited
            }
        } catch (e: CrisisCleanupNetworkException) {
            if (e.errors.condenseMessages.contains("is already a part of this organization")) {
                return OrgInviteResult.Redundant
            }
        }

        return OrgInviteResult.Unknown
    }

    override suspend fun registerOrganization(
        referer: String,
        invite: IncidentOrganizationInviteInfo,
    ): Boolean {
        val registerOrganization = NetworkOrganizationRegistration(
            name = invite.organizationName,
            referral = referer,
            incident = invite.incidentId,
            contact = NetworkOrganizationContact(
                email = invite.emailAddress,
                firstName = invite.firstName,
                lastName = invite.lastName,
                mobile = invite.mobile,
                title = null,
                organization = null,
            ),
        )
        val organization = networkApi.registerOrganization(registerOrganization).organization
        return organization.id > 0 && organization.name.lowercase() == invite.organizationName.lowercase()
    }
}

private data class UserDetails(
    val displayName: String,
    val organizationName: String,
    val avatarUrl: URL?,
)
