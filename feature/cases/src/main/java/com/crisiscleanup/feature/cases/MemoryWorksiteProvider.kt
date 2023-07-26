package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.model.data.EmptyWorksite
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryWorksiteProvider @Inject constructor() : WorksiteProvider {
    override val editableWorksite = MutableStateFlow(EmptyWorksite)
}

@Module
@InstallIn(SingletonComponent::class)
interface WorksiteProviderModule {
    @Binds
    @Singleton
    fun bindsWorksiteProvider(
        provider: MemoryWorksiteProvider
    ): WorksiteProvider
}