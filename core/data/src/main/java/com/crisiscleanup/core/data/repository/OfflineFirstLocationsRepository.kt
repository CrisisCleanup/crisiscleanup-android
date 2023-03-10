package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.database.dao.LocationDao
import com.crisiscleanup.core.database.model.PopulatedLocation
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Location
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstLocationsRepository @Inject constructor(
    private val locationDao: LocationDao,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LocationsRepository {
    override fun streamLocations(ids: List<Long>): Flow<List<Location>> =
        locationDao.streamLocations(ids).map { it.map(PopulatedLocation::asExternalModel) }
}