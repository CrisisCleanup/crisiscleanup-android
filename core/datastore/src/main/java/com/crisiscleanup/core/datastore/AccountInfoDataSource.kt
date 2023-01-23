package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Stores info/data related to the authenticated account
 */
class AccountInfoDataSource @Inject constructor(
    private val dataStore: DataStore<AccountInfo>
) {
    /* UPDATE [AccountInfoDataSourceTest] when changing below */

    val accountData = dataStore.data
        .map {
            val fullName = "${it.firstName} ${it.lastName}".trim()
            val profilePictureUri = if (it.profilePictureUri?.isEmpty() == true) "https://avatars.dicebear.com/api/bottts/$fullName.svg"
            else it.profilePictureUri
            AccountData(
                accessToken = it.accessToken,
                tokenExpiry = Instant.fromEpochSeconds(it.expirySeconds),
                fullName = fullName,
                emailAddress = it.email,
                profilePictureUri = profilePictureUri,
            )
        }

    suspend fun clearAccount() = setAccount("", "", "", "", 0, "")

    suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
        profilePictureUri: String,
    ) {
        dataStore.updateData {
            it.copy {
                this.accessToken = accessToken
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                this.expirySeconds = expirySeconds
                this.profilePictureUri = profilePictureUri
            }
        }
    }
}
