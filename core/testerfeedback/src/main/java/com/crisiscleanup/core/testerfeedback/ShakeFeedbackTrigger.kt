package com.crisiscleanup.core.testerfeedback

import android.app.Activity
import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.crisiscleanup.core.testerfeedbackapi.FeedbackReceiver
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTrigger
import com.squareup.seismic.ShakeDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Modified https://github.com/firebase/firebase-android-sdk/blob/master/firebase-appdistribution/test-app/src/main/kotlin/com/googletest/firebase/appdistribution/testapp/ShakeDetectionFeedbackTrigger.kt
class ShakeFeedbackTrigger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val feedbackReceiver: FeedbackReceiver,
) : ShakeDetector.Listener, DefaultLifecycleObserver, FeedbackTrigger {

    private val shakeDetector =
        ShakeDetector(this).also { it.setSensitivity(ShakeDetector.SENSITIVITY_LIGHT) }

    override fun onResume(owner: LifecycleOwner) {
        val sensorManager = context.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
        shakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause(owner: LifecycleOwner) {
        shakeDetector.stop()
    }

    override fun hearShake() {
        feedbackReceiver.onStartFeedback("shake")
    }
}