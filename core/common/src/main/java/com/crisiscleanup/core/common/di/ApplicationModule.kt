package com.crisiscleanup.core.common.di

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.ApplicationResourceProvider
import com.crisiscleanup.core.common.CommonInputValidator
import com.crisiscleanup.core.common.InputValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
interface ApplicationModule {
    @Singleton
    @Binds
    fun bindsAndroidResourceProvider(
        resourceProvider: ApplicationResourceProvider
    ): AndroidResourceProvider

    @Singleton
    @Binds
    fun bindsInputValidator(
        validator: CommonInputValidator
    ): InputValidator
}