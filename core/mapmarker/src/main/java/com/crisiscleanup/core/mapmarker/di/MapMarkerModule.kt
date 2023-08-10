package com.crisiscleanup.core.mapmarker.di

import com.crisiscleanup.core.mapmarker.*
import com.crisiscleanup.core.model.data.LocationBoundsConverter
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

    @Binds
    fun bindsIncidentBoundsProvider(
        calculator: MapsIncidentBoundsProvider
    ): IncidentBoundsProvider

    @Binds
    fun bindsLocationBoundsConverter(
        converter: CrisisCleanupLocationBoundsConverter
    ): LocationBoundsConverter
}