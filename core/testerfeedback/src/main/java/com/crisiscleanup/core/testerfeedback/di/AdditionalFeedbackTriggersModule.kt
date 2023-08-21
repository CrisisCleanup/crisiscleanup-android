package com.crisiscleanup.core.testerfeedback.di

import com.crisiscleanup.core.testerfeedback.TesterFeedbackTriggerProvider
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTriggerProvider
import com.crisiscleanup.core.testerfeedbackapi.di.FeedbackTriggerProviderKey
import com.crisiscleanup.core.testerfeedbackapi.di.FeedbackTriggerProviders
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AdditionalFeedbackTriggerModule {
    @Binds
    @FeedbackTriggerProviderKey(FeedbackTriggerProviders.Additional)
    fun bindsFeedbackTriggerProvider(provider: TesterFeedbackTriggerProvider): FeedbackTriggerProvider
}
