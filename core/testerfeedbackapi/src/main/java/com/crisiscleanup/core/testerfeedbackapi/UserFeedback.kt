package com.crisiscleanup.core.testerfeedbackapi

import android.os.Bundle

interface FeedbackReceiver {
    fun onStartFeedback(starterKey: String, payload: Bundle = Bundle.EMPTY)
}

interface FeedbackTrigger