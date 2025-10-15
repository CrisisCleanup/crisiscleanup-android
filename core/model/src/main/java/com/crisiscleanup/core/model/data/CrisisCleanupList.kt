package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class CrisisCleanupList(
    val id: Long,
    val updatedAt: Instant,
    val networkId: Long,
    val parentNetworkId: Long?,
    val name: String,
    val description: String,
    val listOrder: Long?,
    val tags: String?,
    val model: ListModel,
    val objectIds: List<Long>,
    val shared: ListShare,
    val permission: ListPermission,
    val incidentId: Long,
    val incident: IncidentIdNameType?,
)

val EmptyList = CrisisCleanupList(
    id = 0,
    updatedAt = Instant.fromEpochSeconds(0),
    networkId = 0,
    parentNetworkId = null,
    name = "",
    description = "",
    listOrder = null,
    tags = null,
    model = ListModel.None,
    objectIds = emptyList(),
    shared = ListShare.Private,
    permission = ListPermission.Read,
    incidentId = EmptyIncident.id,
    incident = EmptyIncidentIdNameType,
)

enum class ListModel(val literal: String) {
    None(""),
    File("file_files"),
    Incident("incident_incidents"),
    List("list_lists"),
    Organization("organization_organizations"),
    OrganizationIncidentTeam("organization_organizations_incidents_teams"),
    User("user_users"),
    Worksite("worksite_worksites"),
}

private val modelLiteralLookup = ListModel.entries.associateBy(ListModel::literal)

fun listModelFromLiteral(literal: String) = modelLiteralLookup[literal] ?: ListModel.None

enum class ListPermission(val literal: String) {
    Read("read_only"),
    ReadCopy("read_copy"),
    ReadWriteCopy("read_write_copy"),
    ReadWriteDeleteCopy("read_write_delete_copy"),
}

private val permissionLiteralLookup = ListPermission.entries.associateBy(ListPermission::literal)

fun listPermissionFromLiteral(literal: String) =
    permissionLiteralLookup[literal] ?: ListPermission.Read

enum class ListShare(val literal: String) {
    All("all"),
    GroupAffiliates("groups_affiliates"),
    Organization("organization"),
    Private("private"),
    Public("public"),
    Team("team"),
}

private val shareLiteralLookup = ListShare.entries.associateBy(ListShare::literal)

fun listShareFromLiteral(literal: String) =
    shareLiteralLookup[literal] ?: ListShare.Private
