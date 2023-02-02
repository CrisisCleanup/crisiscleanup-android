package com.crisiscleanup.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class WorksiteDaoTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus

    private val testIncidents = listOf(
        testIncidentEntity(1, 6525),
        testIncidentEntity(23, 152),
        testIncidentEntity(456, 514),
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            CrisisCleanupDatabase::class.java
        ).build()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db)
    }

    @Before
    fun seedDb() = runTest {
        val incidentDao = db.incidentDao()
        incidentDao.upsertIncidents(testIncidents)
    }

    private suspend fun insertWorksites(
        worksites: List<WorksiteEntity>,
        syncedAt: Instant,
    ): List<WorksiteEntity> {
        return db.withTransaction {
            worksites.map {
                val id = worksiteDao.insertWorksiteRoot(syncedAt, it.networkId, it.incidentId)
                val updated = it.copy(id = id)
                worksiteDao.insertWorksite(updated)
                updated
            }
        }
    }

    // Defines all fields setting updated_at to be relative to createdAt
    private fun testWorksiteFullEntity(
        networkId: Long,
        incidentId: Long,
        createdAt: Instant,
        id: Long = 0
    ) = WorksiteEntity(
        id = id,
        networkId = networkId,
        incidentId = incidentId,
        address = "123 address st",
        autoContactFrequencyT = "enum.enver",
        caseNumber = "case",
        city = "city 123",
        county = "county 123",
        createdAt = createdAt,
        email = "test123@email.com",
        favoriteId = 4134,
        keyWorkTypeType = "key-type-type",
        latitude = 414.353f,
        longitude = -534.15f,
        name = "full worksite",
        phone1 = "345-414-7825",
        phone2 = "835-621-8938",
        plusCode = "code 123",
        postalCode = "83425",
        reportedBy = 7835,
        state = "ED",
        svi = 6.235f,
        what3Words = "what,three,words",
        updatedAt = createdAt.plus(99.seconds),
    )

    private val nowInstant = Clock.System.now()

    private val createdAtA = nowInstant.plus((-854812).seconds)
    private val updatedAtA = createdAtA.plus(78458.seconds)

    // Between createdA and updatedA
    private val createdAtB = createdAtA.plus(3512.seconds)
    private val updatedAtB = createdAtB.plus(452.seconds)

    private fun existingEntity(
        networkId: Long,
        incidentId: Long = 1,
        address: String = "test-address",
        createdAt: Instant? = null
    ) =
        testWorksiteEntity(
            networkId,
            incidentId,
            address,
            updatedAtA,
            createdAt,
        )

    @Test
    fun syncNewWorksites() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            existingEntity(1, createdAt = createdAtA),
            existingEntity(2),
            existingEntity(3, 23, address = "test-address-23"),
        )
        val previousSyncedAt = nowInstant.plus((-9999).seconds)
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)

        // Sync
        val syncingWorksites = listOf(
            testWorksiteEntity(4, 1, "missing-created-at-4", updatedAtB, null),
            testWorksiteFullEntity(5, 1, createdAtB).copy(
                createdAt = null
            ),
            testWorksiteEntity(6, 1, "created-at-6", updatedAtB, createdAtB),
            testWorksiteFullEntity(7, 1, createdAtB),
        )
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(487.seconds)
        worksiteDaoPlus.syncExternalWorksites(1, syncingWorksites, syncedAt)

        // Assert

        var expected = listOf(existingWorksites[2])
        var actual = worksiteDao.getWorksites(23).first().map { it.entity }
        assertEquals(expected, actual)

        // TODO worksites_root data as well

        actual = worksiteDao.getWorksites(1).first().map { it.entity }

        // Order by updated_at desc id desc
        // updatedA > updatedB > fullA.updated_at
        assertEquals(listOf(2L, 1, 6, 4, 7, 5), actual.map(WorksiteEntity::id))

        expected = listOf(
            // First 2 are unchanged
            existingWorksites[1],
            existingWorksites[0],

            // Remaining are as inserted
            syncingWorksites[2].copy(
                id = 6,
            ),
            syncingWorksites[0].copy(
                id = 4,
            ),
            syncingWorksites[3].copy(
                id = 7,
            ),
            syncingWorksites[1].copy(
                id = 5,
            ),
        )
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i], "$i")
        }
    }

    @Test
    fun syncUpdateWorksites() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            existingEntity(1, createdAt = createdAtA),
            existingEntity(2),
            existingEntity(3, 23, address = "test-address-23"),
            existingEntity(4, createdAt = createdAtA),
            existingEntity(5),
            existingEntity(6, createdAt = createdAtA),
            existingEntity(7),
        )
        val previousSyncedAt = nowInstant.plus((-9999).seconds)
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)

        // Sync
        val syncingWorksites = listOf(
            // Not syncing 1
            // Not syncing 2
            // 3 is different incident

            // Modify 4 and 5 should keep original created_at
            testWorksiteEntity(4, 1, "missing-created-at-4", updatedAtB, null),
            testWorksiteFullEntity(5, 1, createdAtB).copy(
                createdAt = null
            ),

            // Modify 6 and 7 should update created_at
            testWorksiteEntity(6, 1, "update-created-at-6", updatedAtB, createdAtB),
            testWorksiteFullEntity(7, 1, createdAtB),
        )
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(487.seconds)
        worksiteDaoPlus.syncExternalWorksites(1, syncingWorksites, syncedAt)

        // Assert

        var expected = listOf(existingWorksites[2])
        var actual = worksiteDao.getWorksites(23).first().map { it.entity }
        assertEquals(expected, actual)

        // TODO worksites_root data as well

        actual = worksiteDao.getWorksites(1).first().map { it.entity }

        // Order by updated_at desc id desc
        // updatedA > updatedB > fullA.updated_at
        assertEquals(listOf(2L, 1, 6, 4, 7, 5), actual.map(WorksiteEntity::id))

        expected = listOf(
            // First 2 are unchanged
            existingWorksites[1],
            existingWorksites[0],

            existingWorksites[5].copy(
                address = "update-created-at-6",
                updatedAt = updatedAtB,
                createdAt = createdAtB,
            ),
            // No change to created_at
            existingWorksites[3].copy(
                address = "missing-created-at-4",
                updatedAt = updatedAtB,
            ),
            testWorksiteFullEntity(7, 1, createdAtB, 7).copy(
                id = 7,
            ),
            testWorksiteFullEntity(5, 1, createdAtB, 5).copy(
                id = 5,
                createdAt = null,
                updatedAt = createdAtB.plus(99.seconds),
            ),
        )
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i], "$i")
        }
    }

    // TODO Sync existing worksite incident changes
}

fun testWorksiteEntity(
    networkId: Long,
    incidentId: Long,
    address: String,
    updatedAt: Instant,
    createdAt: Instant? = null,
    id: Long = 0,
) = WorksiteEntity(
    id = id,
    networkId = networkId,
    incidentId = incidentId,
    address = address,
    autoContactFrequencyT = "",
    caseNumber = "",
    city = "",
    county = "",
    createdAt = createdAt,
    email = "",
    favoriteId = null,
    keyWorkTypeType = "",
    latitude = 0f,
    longitude = 0f,
    name = "",
    phone1 = "",
    phone2 = null,
    plusCode = null,
    postalCode = "",
    reportedBy = 0,
    state = "",
    svi = null,
    what3Words = null,
    updatedAt = updatedAt,
)