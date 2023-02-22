package com.crisiscleanup

import android.app.Activity
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.PasswordRequestCode
import com.crisiscleanup.core.common.log.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

internal class CredentialSaveRetrieveManager(
    private val coroutineScope: CoroutineScope,
    private val credentialManager: CredentialManager,
    private val logger: AppLogger,
) {
    fun passkeySignIn(
        activityWr: WeakReference<Activity>,
        authEventManager: AuthEventManager,
    ) {
        val getPasswordOption = GetPasswordOption()

        val getCredRequest = GetCredentialRequest(
            listOf(getPasswordOption)
        )

        coroutineScope.launch {
            try {
                activityWr.get()?.let { activity ->
                    val result = credentialManager.getCredential(
                        request = getCredRequest,
                        activity = activity,
                    )
                    handleSignIn(result, authEventManager)
                }
            } catch (e: GetCredentialException) {
                if (e !is NoCredentialException) {
                    logger.logException(e)
                }
            }
        }
    }

    private fun handleSignIn(
        result: GetCredentialResponse,
        authEventManager: AuthEventManager,
    ) {
        // Handle the successfully returned credential.
        when (val credential = result.credential) {
            is PasswordCredential -> {
                val username = credential.id
                val password = credential.password
                authEventManager.onPasswordCredentialsResult(
                    username, password, PasswordRequestCode.Success
                )
            }

            else -> {
                // TODO Report unhandled response if significant
                authEventManager.onPasswordCredentialsResult(
                    "", "", PasswordRequestCode.Fail
                )
            }
        }
    }

    fun saveAccountPassword(
        activityWr: WeakReference<Activity>,
        emailAddress: String,
        password: String,
    ) {
        val createPasswordRequest =
            CreatePasswordRequest(id = emailAddress, password = password)

        // Create credentials and handle result.
        coroutineScope.launch {
            try {
                activityWr.get()?.let {
                    credentialManager.createCredential(createPasswordRequest, it)
                }
            } catch (e: CreateCredentialException) {
                logger.logException(e)
            }
        }
    }
}