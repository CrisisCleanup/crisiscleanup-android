package com.crisiscleanup

import android.app.Activity
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.CreateCustomCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.event.PasswordCredentials
import com.crisiscleanup.core.common.event.PasswordRequestCode
import com.crisiscleanup.core.common.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CredentialSaveRetrieveManager(
    private val coroutineScope: CoroutineScope,
    private val credentialManager: CredentialManager,
    private val logger: AppLogger,
) {
    fun passkeySignIn(
        activity: Activity,
        authEventBus: AuthEventBus,
    ) {
        val getPasswordOption = GetPasswordOption()

        val getCredRequest = GetCredentialRequest(
            listOf(getPasswordOption)
        )

        coroutineScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = getCredRequest,
                    activity = activity,
                )
                handleSignIn(result, authEventBus)
            } catch (e: GetCredentialException) {
                if (e !is NoCredentialException) {
                    logger.logException(e)
                }
            }
        }
    }

    private fun handleSignIn(
        result: GetCredentialResponse,
        authEventBus: AuthEventBus,
    ) {
        // Handle the successfully returned credential.
        when (val credential = result.credential) {
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                authEventBus.onPasswordCredentialsResult(
                    PasswordCredentials(username, password, PasswordRequestCode.Success)
                )
            }

            else -> {
                // TODO Report unhandled response if significant
                authEventBus.onPasswordCredentialsResult(
                    PasswordCredentials("", "", PasswordRequestCode.Fail)
                )
            }
        }
    }

    fun saveAccountPassword(
        activity: Activity,
        emailAddress: String,
        password: String,
    ) {
        val createPasswordRequest =
            CreatePasswordRequest(id = emailAddress, password = password)

        // Create credentials and handle result.
        coroutineScope.launch {
            try {
                credentialManager.createCredential(createPasswordRequest, activity)
            } catch (e: CreateCredentialException) {
                logger.logException(e)
                handleFailure(e)
            }
        }
    }

    private fun handleFailure(e: CreateCredentialException) {
        when (e) {
            is CreatePublicKeyCredentialDomException -> {
                // Handle the passkey DOM errors thrown according to the
                // WebAuthn spec.
            }

            is CreateCredentialCancellationException -> {
                // The user intentionally canceled the operation and chose not
                // to register the credential.
            }

            is CreateCredentialInterruptedException -> {
                // Retry-able error. Consider retrying the call.
            }

            is CreateCredentialProviderConfigurationException -> {
                // Your app is missing the provider configuration dependency.
                // Most likely, you're missing the
                // "credentials-play-services-auth" module.
            }

            is CreateCredentialUnknownException -> {
                // Do something
            }

            is CreateCustomCredentialException -> {
                // You have encountered an error from a 3rd-party SDK. If you
                // make the API call with a request object that's a subclass of
                // CreateCustomCredentialRequest using a 3rd-party SDK, then you
                // should check for any custom exception type constants within
                // that SDK to match with e.type. Otherwise, drop or log the
                // exception.
            }

            else -> Log.e(
                "credential-manager",
                "Unexpected exception type ${e::class.java.name}",
                e
            )
        }
    }
}