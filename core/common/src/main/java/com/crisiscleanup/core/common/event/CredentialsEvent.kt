package com.crisiscleanup.core.common.event

interface CredentialsRequestListener {
    fun onCredentialsRequest()
}

interface CredentialsResultListener {
    fun onPasswordCredentialsResult(
        emailAddress: String,
        password: String,
        resultCode: PasswordRequestCode,
    )
}

interface SaveCredentialsListener {
    fun onSaveCredentials(emailAddress: String, password: String)
}

enum class PasswordRequestCode {
    Success,
    Fail,
    PasswordNotFound,
    PasswordAccessDenied,
}