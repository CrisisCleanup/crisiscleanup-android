package com.crisiscleanup.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class IncidentDaoTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var incidentDao: IncidentDao
    private lateinit var incidentDaoPlus: IncidentDaoPlus

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            CrisisCleanupDatabase::class.java
        ).build()
        incidentDao = db.incidentDao()
        incidentDaoPlus = IncidentDaoPlus(db)
    }

    @Test
    fun queryIncidentsByStartDateDesc() = runTest {
        val earliestSeconds = 32423523L
        val middleSeconds = earliestSeconds + 324L
        val latestSeconds = middleSeconds + 5243L
        val incidents = listOf(
            testIncidentEntity(1, middleSeconds),
            testIncidentEntity(2, latestSeconds),
            testIncidentEntity(3, earliestSeconds),
        )
        incidentDao.upsertIncidents(incidents)

        val savedIncidents = incidentDao.getIncidents().first()

        assertEquals(
            listOf(2L, 1, 3),
            savedIncidents.map { it.entity.id }
        )
    }

    private fun testIncidents(): List<IncidentEntity> {
        val seconds = 52352385L
        return listOf(
            testIncidentEntity(48, seconds + 3),
            testIncidentEntity(18, seconds + 1),
            testIncidentEntity(954, seconds + 2),
        )
    }

    private fun makeIncidentLocationCrossRefs(idMap: Map<Long, Set<Long>>): List<IncidentIncidentLocationCrossRef> {
        val incidentCrossRefs = mutableListOf<IncidentIncidentLocationCrossRef>()
        for ((incidentId, locationIds) in idMap.entries) {
            for (locationId in locationIds) {
                incidentCrossRefs.add(
                    IncidentIncidentLocationCrossRef(incidentId, locationId)
                )
            }
        }
        return incidentCrossRefs
    }

    private fun testIncidentDataSet(): Triple<List<IncidentLocationEntity>, List<IncidentEntity>, List<IncidentIncidentLocationCrossRef>> {
        val incidentLocations = listOf(
            IncidentLocationEntity(15, 158),
            IncidentLocationEntity(226, 241),
            IncidentLocationEntity(31, 386),
        )
        val incidents = testIncidents()
        val incidentToIncidentLocations = makeIncidentLocationCrossRefs(
            mapOf(
                48L to setOf(15L, 226),
                18L to setOf(226L, 31),
                954L to setOf(15L),
            )
        )
        return Triple(incidentLocations, incidents, incidentToIncidentLocations)
    }

    /**
     * Incidents are saved with location cross references
     */
    @Test
    fun saveIncidents_queryIncidentsWithLocations() = runTest {
        val (incidentLocations, incidents, incidentToIncidentLocations) = testIncidentDataSet()

        incidentDaoPlus.saveIncidents(
            incidents,
            incidentLocations,
            incidentToIncidentLocations
        )

        val savedIncidents = incidentDao.getIncidents().first()

        assertEquals(
            listOf(
                listOf(15L, 226),
                listOf(15L),
                // Assumes cross reference ordering is asc
                listOf(31L, 226),
            ),
            savedIncidents.map { incident ->
                incident.locations.map(IncidentLocationEntity::id)
            }
        )
    }

    /**
     * Archives incidents with IDs not in specified
     */
    @Test
    fun archiveUnspecified() = runTest {
        incidentDao.upsertIncidents(testIncidents())

        val savedIncidents = incidentDao.getIncidents().first()
        assertEquals(listOf(48L, 954, 18), savedIncidents.map { it.entity.id })

        incidentDao.setExcludedArchived(setOf(48L, 18))

        val updatedIncidents = incidentDao.getIncidents().first()
        assertEquals(listOf(48L, 18), updatedIncidents.map { it.entity.id })
    }
}

fun testIncidentEntity(
    id: Long,
    startAtSeconds: Long,
) = IncidentEntity(id, Instant.fromEpochSeconds(startAtSeconds), "", "")