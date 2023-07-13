package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyOrgData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

// TODO Rewrite tests

/**
 * Stores info/data related to the authenticated account
 */
class AccountInfoDataSource @Inject constructor(
    private val dataStore: DataStore<AccountInfo>,
    private val secureDataSource: SecureDataSource,
    @ApplicationScope private val externalScope: CoroutineScope,
) {
    companion object {
        fun defaultProfilePictureUri(fullName: String): String =
            if (fullName.isEmpty()) ""
            else "https://avatars.dicebear.com/api/bottts/$fullName.svg"
    }

    val accountData = dataStore.data
        .map {
            val fullName = "${it.firstName} ${it.lastName}".trim()
            val profilePictureUri =
                if (it.profilePictureUri?.isEmpty() == true) defaultProfilePictureUri(fullName)
                else it.profilePictureUri
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

    fun clearAccountTokens() {
        // TODO Atomic save
        saveAuthTokens("", "")
        externalScope.launch {
            dataStore.updateData {
                it.copy {
                    this.expirySeconds = 0
                }
            }
        }
    }

    suspend fun clearAccount() {
        setAccount("", "", 0, "", "", "", 0, "", emptyOrgData)
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
                this.orgId = org.id
                this.orgName = org.name
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
}
