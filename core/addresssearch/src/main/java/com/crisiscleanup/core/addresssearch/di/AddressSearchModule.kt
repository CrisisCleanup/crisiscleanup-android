package com.crisiscleanup.core.addresssearch.di

import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.addresssearch.GooglePlaceAddressSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AddressSearchModule {
    @Binds
    fun bindsAddressSearchRepository(
        repository: GooglePlaceAddressSearchRepository
    ): AddressSearchRepository
}