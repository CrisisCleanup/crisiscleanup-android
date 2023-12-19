package com.crisiscleanup.core.model.data

data class InvitationRequest(
    val firstName: String,
    val lastName: String,
    val emailAddress: String,
    val title: String,
    val password: String,
    val mobile: String,
    val languageId: Long,

    val inviterEmailAddress: String,
)

data class IncidentOrganizationInviteInfo(
    val incidentId: Long,
    val organizationName: String,
    val emailAddress: String,
    val mobile: String,
    val firstName: String,
    val lastName: String,
)

data class CodeInviteAccept(
    val firstName: String,
    val lastName: String,
    val emailAddress: String,
    val title: String,
    val password: String,
    val mobile: String,
    val languageId: Long,

    val invitationCode: String,
)
