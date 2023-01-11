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
    // Update AccountInfoDataSourceTest when changing below

    private val dataStore: DataStore<AccountInfo>
) {
    val accountData = dataStore.data
        .map {
            AccountData(
                accessToken = it.accessToken,
                tokenExpiry = Instant.fromEpochSeconds(it.expirySeconds),
                displayName = "${it.firstName} ${it.lastName}".trim(),
            )
        }

    suspend fun clearAccount() {
        setAccount("", "", "", "", 0)
    }

    suspend fun setAccount(
        accessToken: String,
        email: String,
        firstName: String,
        lastName: String,
        expirySeconds: Long,
    ) {
        dataStore.updateData {
            it.copy {
                this.accessToken = accessToken
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                this.expirySeconds = expirySeconds
            }
        }
    }
}
