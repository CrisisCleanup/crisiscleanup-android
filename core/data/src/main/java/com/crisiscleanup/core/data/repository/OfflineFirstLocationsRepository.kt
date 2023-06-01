package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.LocationDao
import com.crisiscleanup.core.database.model.PopulatedLocation
import com.crisiscleanup.core.database.model.asExternalModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstLocationsRepository @Inject constructor(
    private val locationDao: LocationDao,
) : LocationsRepository {
    override fun streamLocations(ids: Collection<Long>) =
        locationDao.streamLocations(ids).map { it.map(PopulatedLocation::asExternalModel) }

    override fun getLocations(ids: Collection<Long>) =
        locationDao.getLocations(ids).map(PopulatedLocation::asExternalModel)
}