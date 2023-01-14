package com.crisiscleanup.feature.authentication.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.emptyAccountData

class LoginInputData {
    var emailAddress by mutableStateOf("")
    var password by mutableStateOf("")
}

data class AuthenticationState(
    val accountData: AccountData = emptyAccountData,
    val hasAccessToken: Boolean = false,
    val isTokenExpired: Boolean = false,
)