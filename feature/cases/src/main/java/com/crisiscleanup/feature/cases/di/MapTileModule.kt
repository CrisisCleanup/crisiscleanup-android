package com.crisiscleanup.feature.cases.di

import com.crisiscleanup.feature.cases.map.CaseDotsMapTileRenderer
import com.crisiscleanup.feature.cases.map.CasesOverviewMapTileRenderer
import com.google.android.gms.maps.model.TileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MapTileModule {
    @Singleton
    @Binds
    fun bindsTileRenderer(
        renderer: CaseDotsMapTileRenderer
    ): CasesOverviewMapTileRenderer

    @Singleton
    @Binds
    fun bindsTileProvider(provider: CaseDotsMapTileRenderer): TileProvider
}