package com.crisiscleanup.feature.authentication.di

import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.event.CrisisCleanupAuthEventBus
import com.crisiscleanup.feature.authentication.AccessTokenDecoder
import com.crisiscleanup.feature.authentication.JwtDecoder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AuthenticationModule {
    @Binds
    fun bindsAccessTokenDecoder(tokenDecoder: JwtDecoder): AccessTokenDecoder

    @Binds
    fun bindsAuthEventBus(eventBus: CrisisCleanupAuthEventBus): AuthEventBus
}
