package com.crisiscleanup.feature.authentication

import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.network.model.NetworkUserProfile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

suspend fun AccountDataRepository.setAccount(
    accountProfile: NetworkUserProfile,
    refreshToken: String,
    accessToken: String,
    expiresIn: Int,
) = with(accountProfile) {
    setAccount(
        refreshToken = refreshToken,
        accessToken = accessToken,
        id = id,
        email = email,
        phone = mobile,
        firstName = firstName,
        lastName = lastName,
        expirySeconds = Clock.System.now().plus(expiresIn.seconds).epochSeconds,
        profilePictureUri = profilePicUrl ?: "",
        org = OrgData(
            id = organization.id,
            name = organization.name,
        ),
        hasAcceptedTerms = hasAcceptedTerms == true,
        approvedIncidentIds = approvedIncidents,
        activeRoles = activeRoles,
    )
}
