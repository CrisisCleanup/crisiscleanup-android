package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyOrgData
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Stores info/data related to the authenticated account
 */
class AccountInfoDataSource @Inject constructor(
    private val dataStore: DataStore<AccountInfo>,
) {
    companion object {
        fun defaultProfilePictureUri(fullName: String): String =
            if (fullName.isEmpty()) ""
            else "https://avatars.dicebear.com/api/bottts/$fullName.svg"
    }

    // UPDATE AccountInfoDataSourceTest (and downstream tests) when changing below

    val accountData = dataStore.data
        .map {
            val fullName = "${it.firstName} ${it.lastName}".trim()
            val profilePictureUri =
                if (it.profilePictureUri?.isEmpty() == true) defaultProfilePictureUri(fullName)
                else it.profilePictureUri
            AccountData(
                id = it.id,
                accessToken = it.accessToken,
                tokenExpiry = Instant.fromEpochSeconds(it.expirySeconds),
                fullName = fullName,
                emailAddress = it.email,
                profilePictureUri = profilePictureUri,
                org = OrgData(
                    id = it.orgId,
                    name = it.orgName,
                )
            )
        }

    suspend fun clearAccount() = setAccount(0, "", "", "", "", 0, "", emptyOrgData)

    suspend fun setAccount(
        id: Long,
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
        org: OrgData,
    ) {
        dataStore.updateData {
            it.copy {
                this.id = id
                this.accessToken = accessToken
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
}
