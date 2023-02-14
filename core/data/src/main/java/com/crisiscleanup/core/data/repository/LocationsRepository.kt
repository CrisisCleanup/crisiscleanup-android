package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Location
import kotlinx.coroutines.flow.Flow

interface LocationsRepository {
    fun streamLocations(ids: List<Long>): Flow<List<Location>>
}