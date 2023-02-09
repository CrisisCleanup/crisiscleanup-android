package com.crisiscleanup.core.mapmarker.di

import com.crisiscleanup.core.mapmarker.InMemoryDotProvider
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface MapMarkerModule {
    @Binds
    fun bindsMapMarkerProvider(provider: InMemoryDotProvider): MapCaseDotProvider
}