package com.crisiscleanup.di

import com.crisiscleanup.FirebaseFeedbackReceiver
import com.crisiscleanup.core.testerfeedbackapi.FeedbackReceiver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FeedbackModule {
    @Binds
    fun bindsFeedbackReceiver(receiver: FirebaseFeedbackReceiver): FeedbackReceiver
}
