package com.crisiscleanup.core.mapmarker.di

import com.crisiscleanup.core.mapmarker.InMemoryDotProvider
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.WorkTypeIconProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MapMarkerModule {
    @Singleton
    @Binds
    fun bindsMapMarkerProvider(provider: InMemoryDotProvider): MapCaseDotProvider

    @Singleton
    @Binds
    fun bindsMapIconProvider(provider: WorkTypeIconProvider): MapCaseIconProvider
}