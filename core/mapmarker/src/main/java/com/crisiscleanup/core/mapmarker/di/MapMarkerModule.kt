package com.crisiscleanup.core.mapmarker.di

import com.crisiscleanup.core.mapmarker.*
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

    @Singleton
    @Binds
    fun bindsBitmapIconProvider(
        provider: CrisisCleanupDrawableResourceBitmapProvider
    ): DrawableResourceBitmapProvider
}