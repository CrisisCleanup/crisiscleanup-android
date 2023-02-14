package com.crisiscleanup.feature.authentication.di

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.CrisisCleanupAuthEventManager
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
    fun bindsAuthEventManager(
        eventManager: CrisisCleanupAuthEventManager
    ): AuthEventManager
}
