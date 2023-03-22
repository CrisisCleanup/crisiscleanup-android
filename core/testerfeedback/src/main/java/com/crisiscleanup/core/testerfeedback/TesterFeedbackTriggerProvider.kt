package com.crisiscleanup.core.testerfeedback

import com.crisiscleanup.core.testerfeedbackapi.FeedbackTrigger
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTriggerProvider
import javax.inject.Inject

class TesterFeedbackTriggerProvider @Inject constructor(
    shakeTrigger: ShakeFeedbackTrigger,
    screenshotTrigger: ScreenshotFeedbackTrigger,
) : FeedbackTriggerProvider {
    override val triggers: Collection<FeedbackTrigger> = listOf(shakeTrigger, screenshotTrigger)
}