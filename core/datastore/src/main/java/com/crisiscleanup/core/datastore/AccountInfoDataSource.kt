package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyOrgData
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

// TODO Rewrite tests

/**
 * Stores info/data related to the authenticated account
 */
class AccountInfoDataSource @Inject constructor(
    private val dataStore: DataStore<AccountInfo>,
    private val secureDataSource: SecureDataSource,
    private val appEnv: AppEnv,
) {
    companion object {
        fun defaultProfilePictureUri(fullName: String): String =
            if (fullName.isEmpty()) {
                ""
            } else {
                "https://avatars.dicebear.com/api/bottts/$fullName.svg"
            }
    }

    val accountData = dataStore.data
        .map {
            val fullName = "${it.firstName} ${it.lastName}".trim()
            val profilePictureUri =
                if (it.profilePictureUri?.isEmpty() == true) {
                    defaultProfilePictureUri(fullName)
                } else {
                    it.profilePictureUri
                }
            AccountData(
                id = it.id,
                tokenExpiry = Instant.fromEpochSeconds(it.expirySeconds),
                fullName = fullName,
                emailAddress = it.email,
                profilePictureUri = profilePictureUri,
                org = OrgData(
                    id = it.orgId,
                    name = it.orgName,
                ),
                hasAcceptedTerms = it.hasAcceptedTerms,
                approvedIncidents = it.approvedIncidentsMap.keys,
                areTokensValid = refreshToken.isNotBlank(),
            )
        }

    val refreshToken: String
        get() = secureDataSource.refreshToken ?: ""

    val accessToken: String
        get() = secureDataSource.accessToken ?: ""

    private fun saveAuthTokens(refreshToken: String, accessToken: String) {
        secureDataSource.saveAuthTokens(refreshToken, accessToken)
    }

    suspend fun clearAccount() {
        setAccount("", "", 0, "", "", "", 0, "", emptyOrgData, false, emptySet())
    }

    suspend fun setAccount(
        refreshToken: String,
        accessToken: String,
        id: Long,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
        org: OrgData,
        hasAcceptedTerms: Boolean,
        approvedIncidentIds: Set<Long>,
    ) {
        // TODO Atomic save
        saveAuthTokens(refreshToken, accessToken)
        dataStore.updateData {
            it.copy {
                this.id = id
                // Access token source of truth is in the secure store
                this.accessToken = ""
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                this.expirySeconds = expirySeconds
                this.profilePictureUri = profilePictureUri
                orgId = org.id
                orgName = org.name
                this.hasAcceptedTerms = hasAcceptedTerms
                this.approvedIncidents.clear()
                approvedIncidentIds.forEach { id -> this.approvedIncidents[id] = true }
            }
        }
    }

    suspend fun updateAccountTokens(
        refreshToken: String,
        accessToken: String,
        expirySeconds: Long,
    ) {
        // TODO Atomic save
        saveAuthTokens(refreshToken, accessToken)
        dataStore.updateData {
            it.copy {
                this.expirySeconds = expirySeconds
            }
        }
    }

    suspend fun update(
        pictureUri: String?,
        isAcceptedTerms: Boolean,
        incidentIds: Set<Long>,
    ) {
        dataStore.updateData {
            it.copy {
                pictureUri?.let { p -> profilePictureUri = p }
                hasAcceptedTerms = isAcceptedTerms
                this.approvedIncidents.clear()
                incidentIds.forEach { id -> this.approvedIncidents[id] = true }
            }
        }
    }

    private val skipChangeGuard = AtomicBoolean(false)
    fun ignoreNextAccountChange() {
        if (appEnv.isNotProduction) {
            skipChangeGuard.set(true)
        }
    }
}
