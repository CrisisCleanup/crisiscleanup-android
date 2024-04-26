package com.crisiscleanup

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.crisiscleanup.core.common.PhoneNumberPicker
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPhoneNumberPicker @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) : DefaultLifecycleObserver, PhoneNumberPicker {
    override val phoneNumbers = MutableSharedFlow<String>()

    private var resultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    private val nonDigitRegex = """\D""".toRegex()

    override fun onCreate(owner: LifecycleOwner) {
        (owner as? ComponentActivity)?.let { activity ->
            val contract = ActivityResultContracts.StartIntentSenderForResult()
            resultLauncher = activity.registerForActivityResult(contract) { result ->
                try {
                    val phoneNumber = Identity.getSignInClient(activity)
                        .getPhoneNumberFromIntent(result.data)
                    onPhoneNumber(phoneNumber)
                } catch (e: Exception) {
                    logger.logDebug("Failed to retrieve phone number from system result: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        resultLauncher?.unregister()
    }

    override fun requestPhoneNumber() {
        resultLauncher?.let { launcher ->
            val intentRequest = GetPhoneNumberHintIntentRequest.builder().build()
            Identity.getSignInClient(context)
                .getPhoneNumberHintIntent(intentRequest)
                .addOnSuccessListener { pendingIntent ->
                    try {
                        val senderRequest = IntentSenderRequest.Builder(pendingIntent).build()
                        launcher.launch(senderRequest)
                    } catch (e: Exception) {
                        logger.logDebug("Failed to launch send intent for phone number: ${e.message}")
                    }
                }
                .addOnFailureListener {
                    logger.logDebug("Get phone number hint failed: ${it.message}")
                }
        }
    }

    private fun onPhoneNumber(phoneNumber: String) {
        externalScope.launch {
            phoneNumbers.emit(phoneNumber.replace(nonDigitRegex, ""))
        }
    }
}