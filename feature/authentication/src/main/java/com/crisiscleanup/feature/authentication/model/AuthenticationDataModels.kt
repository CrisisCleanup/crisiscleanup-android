package com.crisiscleanup.feature.authentication.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.emptyAccountData

class LoginInputData(emailAddress: String = "", password: String = "") {
    var emailAddress by mutableStateOf(emailAddress)
    var password by mutableStateOf(password)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoginInputData

        if (emailAddress != other.emailAddress) return false
        if (password != other.password) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

data class AuthenticationState(
    val accountData: AccountData = emptyAccountData,
    val hasAccessToken: Boolean = false,
    val isTokenExpired: Boolean = false,
)