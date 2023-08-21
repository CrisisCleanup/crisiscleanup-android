package com.crisiscleanup.core.common.di

import com.crisiscleanup.core.common.JavaUuidGenerator
import com.crisiscleanup.core.common.UuidGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface IdGeneratorModule {
    @Binds
    fun bindsUuidGenerator(generator: JavaUuidGenerator): UuidGenerator
}
