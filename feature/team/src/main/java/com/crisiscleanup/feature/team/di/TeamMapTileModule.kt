package com.crisiscleanup.feature.team.di

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.commoncase.map.CaseDotsMapTileRenderer
import com.crisiscleanup.core.commoncase.map.CasesOverviewMapTileRenderer
import com.crisiscleanup.core.data.di.CasesFilterType
import com.crisiscleanup.core.data.di.CasesFilterTypes
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseDotProvider
import com.google.android.gms.maps.model.TileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TeamMapTileModule {
    @CasesFilterType(CasesFilterTypes.TeamCases)
    @Singleton
    @Provides
    fun providesTileRenderer(
        resourceProvider: AndroidResourceProvider,
        worksitesRepository: WorksitesRepository,
        mapCaseDotProvider: MapCaseDotProvider,
        appEnv: AppEnv,
    ): CasesOverviewMapTileRenderer = CaseDotsMapTileRenderer(
        useTeamFilters = true,
        resourceProvider,
        worksitesRepository,
        mapCaseDotProvider,
        appEnv,
    )

    @CasesFilterType(CasesFilterTypes.TeamCases)
    @Singleton
    @Provides
    fun providesTileProvider(
        @CasesFilterType(CasesFilterTypes.TeamCases)
        renderer: CasesOverviewMapTileRenderer,
    ): TileProvider = renderer as TileProvider
}
