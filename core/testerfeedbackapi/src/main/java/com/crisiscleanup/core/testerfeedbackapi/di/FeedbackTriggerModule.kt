package com.crisiscleanup.core.testerfeedbackapi.di

import com.crisiscleanup.core.testerfeedbackapi.EmptyFeedbackTriggerProvider
import com.crisiscleanup.core.testerfeedbackapi.FeedbackTriggerProvider
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.*
import javax.inject.Qualifier
import kotlin.jvm.optionals.getOrNull

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FeedbackTriggerProviderKey(val providers: FeedbackTriggerProviders)

enum class FeedbackTriggerProviders {
    Default,
    Additional,
}

@Module
@InstallIn(SingletonComponent::class)
object FeedbackTriggerModule {
    @Provides
    @FeedbackTriggerProviderKey(FeedbackTriggerProviders.Default)
    fun providesFeedbackTriggers(
        @FeedbackTriggerProviderKey(FeedbackTriggerProviders.Additional) triggerProvider: Optional<FeedbackTriggerProvider>
    ): FeedbackTriggerProvider = triggerProvider.getOrNull() ?: EmptyFeedbackTriggerProvider()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdditionalFeedbackTriggerModule {
    @BindsOptionalOf
    @FeedbackTriggerProviderKey(FeedbackTriggerProviders.Additional)
    abstract fun providesFeedbackTriggerProvider(): FeedbackTriggerProvider
}