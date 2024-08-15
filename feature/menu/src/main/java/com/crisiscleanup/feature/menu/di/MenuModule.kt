package com.crisiscleanup.feature.menu.di

import com.crisiscleanup.core.common.CrisisCleanupTutorialDirectors
import com.crisiscleanup.core.common.TutorialDirector
import com.crisiscleanup.core.common.Tutorials
import com.crisiscleanup.feature.menu.MenuTutorialDirector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MenuModule {
    @Singleton
    @Binds
    @Tutorials(CrisisCleanupTutorialDirectors.Menu)
    fun bindsTutorialDirector(
        director: MenuTutorialDirector,
    ): TutorialDirector
}
