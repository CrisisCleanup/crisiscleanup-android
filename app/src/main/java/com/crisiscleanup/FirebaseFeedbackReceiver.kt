package com.crisiscleanup

import android.os.Bundle
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.testerfeedbackapi.FeedbackReceiver
import com.google.firebase.appdistribution.ktx.appDistribution
import com.google.firebase.ktx.Firebase
import javax.inject.Inject

class FirebaseFeedbackReceiver @Inject constructor(
    private val appEnv: AppEnv,
) : FeedbackReceiver {
    override fun onStartFeedback(starterKey: String, payload: Bundle) {
        if (appEnv.isProduction) {
            return
        }

        Firebase.appDistribution.startFeedback("Do share O_o")
    }
}
