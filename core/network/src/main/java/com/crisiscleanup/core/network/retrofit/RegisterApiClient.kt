package com.crisiscleanup.core.network.retrofit

import android.util.Log
import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.isPast
import com.crisiscleanup.core.model.data.CodeInviteAccept
import com.crisiscleanup.core.model.data.ExpiredNetworkOrgInvite
import com.crisiscleanup.core.model.data.IncidentOrganizationInviteInfo
import com.crisiscleanup.core.model.data.InvitationRequest
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import com.crisiscleanup.core.network.CrisisCleanupRegisterApi
import com.crisiscleanup.core.network.model.NetworkAcceptCodeInvite
import com.crisiscleanup.core.network.model.NetworkAcceptPersistentInvite
import com.crisiscleanup.core.network.model.NetworkAcceptedCodeInvitationRequest
import com.crisiscleanup.core.network.model.NetworkAcceptedInvitationRequest
import com.crisiscleanup.core.network.model.NetworkAcceptedPersistentInvite
import com.crisiscleanup.core.network.model.NetworkCreateOrgInvitation
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
    @GET("persistent_invitations")
    suspend fun persistentInvitationInfo(
        @Path("code") inviteCode: String,
    ): NetworkPersistentInvitationResult

    @GET("users/{user}")
    suspend fun noAuthUser(
        @Path("user") userId: Long,
    ): NetworkUser

    @GET("organizations/{organization}")
    suspend fun noAuthOrganization(
        @Path("organization") organizationId: Long,
    ): NetworkOrganizationShort

    @POST("invitations/accept")
    suspend fun acceptInvitationFromCode(
        @Body acceptInvite: NetworkAcceptCodeInvite,
    ): NetworkAcceptedCodeInvitationRequest

    @TokenAuthenticationHeader
    @WrapResponseHeader("invite")
    @POST("persistent_invitations")
    suspend fun createPersistentInvitation(
        @Body org: NetworkCreateOrgInvitation,
    ): NetworkPersistentInvitationResult

    @POST("persistent_invitations/accept")
    suspend fun acceptPersistentInvitation(
        @Body acceptInvite: NetworkAcceptPersistentInvite,
    ): NetworkAcceptedPersistentInvite

    @TokenAuthenticationHeader
    @WrapResponseHeader("invite")
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

    override suspend fun registerOrgVolunteer(invite: InvitationRequest): NetworkAcceptedInvitationRequest {
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
        return networkApi.requestInvitation(inviteRequest)
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

    override suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo? {
        networkApi.persistentInvitationInfo(invite.inviteToken).invite?.let { persistentInvite ->
            if (persistentInvite.expiresAt.isPast) {
                return ExpiredNetworkOrgInvite
            }

            val userDetails = getUserDetails(persistentInvite.id)
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

    override suspend fun getInvitationInfo(inviteCode: String): OrgUserInviteInfo? {
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
        organizationId: Long,
        userId: Long,
    ): NetworkPersistentInvitation {
        val invitation = NetworkCreateOrgInvitation(
            model = "organization_organizations",
            createdBy = userId,
            organizationId = organizationId,
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
        val response = networkApi.acceptPersistentInvitation(payload)
        return when (response.detail) {
            "You have been added to the organization." -> JoinOrgResult.Success
            "User already a member of this organization." -> JoinOrgResult.Redundant
            else -> JoinOrgResult.Unknown
        }
    }

    override suspend fun inviteToOrganization(
        emailAddress: String,
        organizationId: Long?,
    ): Boolean {
        val invite =
            networkApi.inviteToOrganization(NetworkOrganizationInvite(emailAddress, organizationId))
        Log.w("invite", "Invite $invite $emailAddress")
        return invite.invite?.inviteeEmail == emailAddress
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
