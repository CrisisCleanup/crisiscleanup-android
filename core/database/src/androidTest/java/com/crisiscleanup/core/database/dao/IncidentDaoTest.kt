package com.crisiscleanup.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.IncidentsCrossReference
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.asExternalModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class IncidentDaoTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var incidentDao: IncidentDao
    private lateinit var incidentsCrossReference: IncidentsCrossReference

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            CrisisCleanupDatabase::class.java
        ).build()
        incidentDao = db.incidentDao()
        incidentsCrossReference = IncidentsCrossReference(db)
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
            savedIncidents.map { it.asExternalModel().id }
        )
    }

    @Test
    fun queryIncidentsWithLocations() = runTest {
        val incidentLocations = listOf(
            IncidentLocationEntity(15, 158),
            IncidentLocationEntity(226, 241),
            IncidentLocationEntity(31, 386),
        )
        val seconds = 52352385L
        val incidents = listOf(
            testIncidentEntity(48, seconds + 3),
            testIncidentEntity(18, seconds + 1),
            testIncidentEntity(954, seconds + 2),
        )
        val incidentToIncidentLocations = mapOf(
            48L to setOf(15L, 226),
            18L to setOf(226L, 31),
            954L to setOf(15L),
        )

        incidentsCrossReference.saveIncidents(
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
                incident.locations.map { it.id }
            }
        )
    }
}

fun testIncidentEntity(
    id: Long,
    startAtSeconds: Long,
) = IncidentEntity(id, Instant.fromEpochSeconds(startAtSeconds), "", "")