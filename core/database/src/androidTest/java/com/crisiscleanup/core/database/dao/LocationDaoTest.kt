package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.model.PopulatedLocation
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Location
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LocationDaoTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var locationDao: LocationDao
    private lateinit var locationDaoPlus: LocationDaoPlus

    @Before
    fun createDb() {
        db = TestUtil.getDatabase()
        locationDao = db.locationDao()
        locationDaoPlus = LocationDaoPlus(db)
    }

    @Test
    fun cacheQueryLocations() = runTest {
        val locationsSource = listOf(
            LocationEntitySource(1, "Point", listOf(-3.4, 5.1), null),
            LocationEntitySource(2, "Polygon", listOf(-13.4, 55.1, 41.2, -81.2), null),
            LocationEntitySource(
                3, "MultiPolygon", null,
                listOf(
                    listOf(-5.3, 14.5, 82.24, 4.14),
                    listOf(51.28, 42.1, 48.123, -1.88, 6.42, -7.14),
                ),
            ),
            LocationEntitySource(4, "Point", null, null),
            LocationEntitySource(5, "Triangle", listOf(-51.342, -9.3413), null),
        )

        locationDaoPlus.saveLocations(locationsSource)

        val locations =
            locationDao.streamLocations(listOf(1, 2, 3, 4, 5)).first()
                .map(PopulatedLocation::asExternalModel)
        val expecteds = listOf(
            Location(1, "Point", listOf(-3.4, 5.1), null),
            Location(2, "Polygon", listOf(-13.4, 55.1, 41.2, -81.2), null),
            Location(
                3, "MultiPolygon", null, listOf(
                    listOf(-5.3, 14.5, 82.24, 4.14),
                    listOf(51.28, 42.1, 48.123, -1.88, 6.42, -7.14),
                )
            ),
            Location(5, "Triangle", listOf(-51.342, -9.3413), null),
        )
        assertEquals(expecteds, locations)

    }
}