package com.crisiscleanup.core.testerfeedbackapi

interface FeedbackTriggerProvider {
    val triggers: Collection<FeedbackTrigger>
}

class EmptyFeedbackTriggerProvider : FeedbackTriggerProvider {
    override val triggers = emptyList<FeedbackTrigger>()
}
