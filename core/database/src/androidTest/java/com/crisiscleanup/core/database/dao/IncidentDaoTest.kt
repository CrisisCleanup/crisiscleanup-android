package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.PopulatedIncident
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class IncidentDaoTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var incidentDao: IncidentDao
    private lateinit var incidentDaoPlus: IncidentDaoPlus

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
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

        val savedIncidents = incidentDao.streamIncidents().first()

        assertEquals(
            listOf(2L, 1, 3),
            savedIncidents.map { it.entity.id },
        )
    }

    private val testStartAtSeconds = 52352385L
    private fun testIncidents(): List<IncidentEntity> {
        return listOf(
            testIncidentEntity(48, testStartAtSeconds + 3),
            testIncidentEntity(18, testStartAtSeconds + 1),
            testIncidentEntity(954, testStartAtSeconds + 2),
        )
    }

    private fun makeIncidentLocationCrossRefs(idMap: Map<Long, Set<Long>>): List<IncidentIncidentLocationCrossRef> {
        val incidentCrossRefs = mutableListOf<IncidentIncidentLocationCrossRef>()
        for ((incidentId, locationIds) in idMap.entries) {
            for (locationId in locationIds) {
                incidentCrossRefs.add(
                    IncidentIncidentLocationCrossRef(incidentId, locationId),
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
            ),
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
            incidentToIncidentLocations,
        )

        val savedIncidents = incidentDao.streamIncidents().first()

        assertEquals(
            listOf(
                listOf(15L, 226),
                listOf(15L),
                // Assumes cross reference ordering is asc
                listOf(31L, 226),
            ),
            savedIncidents.map { incident ->
                incident.locations.map(IncidentLocationEntity::id)
            },
        )
    }

    /**
     * Incident phone numbers save and load as expected
     */
    @Test
    fun saveIncidentPhoneNumbers() = runTest {
        val nowInstant = Clock.System.now()
        fun phoneIncidentEntity(
            id: Long,
            phoneNumber: String?,
        ) = IncidentEntity(
            id = id,
            startAt = nowInstant,
            activePhoneNumber = phoneNumber,
            name = "",
            shortName = "",
            caseLabel = "",
            type = "",
        )

        // Insert existing
        val existingIncidents = listOf(
            phoneIncidentEntity(1, null),
            phoneIncidentEntity(2, "one, two"),
            phoneIncidentEntity(3, ""),
            phoneIncidentEntity(4, "existing-phone,second-phone"),
        )
        incidentDao.upsertIncidents(existingIncidents)

        // Save
        incidentDao.upsertIncidents(
            listOf(
                // Not defined to multiple numbers
                phoneIncidentEntity(1, "phone,changed"),
                // Multiple numbers to not defined
                phoneIncidentEntity(4, null),
                // New incidents
                phoneIncidentEntity(5, "new-incident"),
                phoneIncidentEntity(6, "phone-1, phone-2"),
            ),
        )

        // Assert
        fun expectedIncident(id: Long, phoneNumbers: List<String>) = Incident(
            id,
            "",
            "",
            "",
            emptyList(),
            phoneNumbers,
            emptyList(),
            false,
        )

        val expecteds = listOf(
            expectedIncident(1, listOf("phone", "changed")),
            expectedIncident(2, listOf("one", "two")),
            expectedIncident(3, listOf()),
            expectedIncident(4, listOf()),
            expectedIncident(5, listOf("new-incident")),
            expectedIncident(6, listOf("phone-1", "phone-2")),
        ).reversed()
        val savedIncidents =
            incidentDao.streamIncidents().first().map(PopulatedIncident::asExternalModel)
        for (i in expecteds.indices) {
            assertEquals(expecteds[i], savedIncidents[i], "$i")
        }
    }

    /**
     * Incidents with changes in locations must not retain previous locations
     */
    @Test
    fun updateIncident_updatesLocationChanges() = runTest {
        val (incidentLocations, incidents, incidentToIncidentLocations) = testIncidentDataSet()

        // Incidents with locations
        incidentDaoPlus.saveIncidents(
            incidents,
            incidentLocations,
            incidentToIncidentLocations,
        )
        // Incident without location
        incidentDao.upsertIncidents(
            listOf(testIncidentEntity(35L, testStartAtSeconds + 11)),
        )

        // incidentToIncidentLocations IDs
        //   48L to setOf(15L, 226),
        //   18L to setOf(226L, 31),
        //   954L to setOf(15L),

        // Sync incidents with different locations
        val syncingIncidents = listOf(
            // Remove all locations
            testIncidentEntity(48L, testStartAtSeconds + 21),
            // Remove one location
            testIncidentEntity(18L, testStartAtSeconds + 22),
            // No change to 954L
            // Change locations
            testIncidentEntity(35L, testStartAtSeconds + 23),
        )
        val syncingLocations = listOf(
            IncidentLocationEntity(321, 985),
            IncidentLocationEntity(852, 164),
        )
        val syncingCrossRefs = makeIncidentLocationCrossRefs(
            mapOf(
                18L to setOf(31L),
                35L to setOf(321L, 852),
            ),
        )
        incidentDaoPlus.saveIncidents(
            syncingIncidents,
            syncingLocations,
            syncingCrossRefs,
        )

        val savedIncidents = incidentDao.streamIncidents().first()

        assertEquals(
            // Sorted by start_by desc
            listOf(
                // Assumes cross reference ordering is asc
                listOf(321L, 852),
                listOf(31L),
                listOf(),
                listOf(15L),
            ),
            savedIncidents.map { incident ->
                incident.locations.map(IncidentLocationEntity::id)
            },
        )
    }

    /**
     * Archives incidents with IDs not in specified
     */
    @Test
    fun archiveUnspecified() = runTest {
        incidentDao.upsertIncidents(testIncidents())

        val savedIncidents = incidentDao.streamIncidents().first()
        assertEquals(listOf(48L, 954, 18), savedIncidents.map { it.entity.id })

        db.testIncidentDao().setExcludedArchived(setOf(48L, 18))

        val updatedIncidents = incidentDao.streamIncidents().first()
        assertEquals(listOf(48L, 18), updatedIncidents.map { it.entity.id })
    }
}

fun testIncidentEntity(
    id: Long,
    startAtSeconds: Long,
) = IncidentEntity(
    id,
    Instant.fromEpochSeconds(startAtSeconds),
    "",
    "",
    "",
    "",
)
