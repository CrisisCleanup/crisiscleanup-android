package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.Location

interface LocationsRepository {
    suspend fun getLocations(ids: Collection<Long>): List<Location>
}