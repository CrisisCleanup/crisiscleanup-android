package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil.testIncidents
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class WorksiteDaoTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus

    @Before
    fun createDb() {
        db = TestUtil.getDatabase()
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
    ) = WorksiteTestUtil.insertWorksites(
        db,
        syncedAt,
        *worksites.toTypedArray(),
    )


    private val nowInstant = Clock.System.now()

    private val previousSyncedAt = nowInstant.plus((-9999).seconds)

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

    /**
     * Syncing worksites data insert into db without fail
     */
    @Test
    fun syncNewWorksites() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            existingEntity(1, createdAt = createdAtA),
            existingEntity(2),
            existingEntity(3, 23, address = "test-address-23"),
        )
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
        val syncingWorkTypes = syncingWorksites.map { emptyList<WorkTypeEntity>() }
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(487.seconds)
        worksiteDaoPlus.syncWorksites(1, syncingWorksites, syncingWorkTypes, syncedAt)

        // Assert

        var expected = listOf(existingWorksites[2])
        var actual = worksiteDao.streamWorksites(23, 99).first().map { it.entity }
        assertEquals(expected, actual)

        actual = worksiteDao.streamWorksites(1, 99).first().map { it.entity }

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

    /**
     * Synced updates to worksite data overwrite as expected
     *
     * - created_at does not overwrite if syncing data is null
     */
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
        val syncingWorkTypes = syncingWorksites.map { emptyList<WorkTypeEntity>() }
        // Sync new and existing
        val syncedAt = previousSyncedAt.plus(487.seconds)
        worksiteDaoPlus.syncWorksites(1, syncingWorksites, syncingWorkTypes, syncedAt)

        // Assert

        var expected = listOf(existingWorksites[2])
        var actual = worksiteDao.streamWorksites(23, 99).first().map { it.entity }
        assertEquals(expected, actual)

        actual = worksiteDao.streamWorksites(1, 99).first().map { it.entity }

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

    /**
     * Full and short entity data must be different for test integrity
     *
     * Update as fields change.
     */
    @Test
    fun testWorksiteEntitiesAreDifferent() {
        val networkId = 41L
        val incidentId = 53L
        val full = testWorksiteFullEntity(networkId, incidentId, createdAtA)
        val short = testWorksiteShortEntity(networkId, incidentId, createdAtA)
        assertNotEquals(full.address, short.address)
        assertNotEquals(full.caseNumber, short.caseNumber)
        assertNotEquals(full.city, short.city)
        assertNotEquals(full.county, short.county)
        assertNotEquals(full.favoriteId, short.favoriteId)
        assertNotEquals(full.keyWorkTypeType, short.keyWorkTypeType)
        assertNotEquals(full.latitude, short.latitude)
        assertNotEquals(full.longitude, short.longitude)
        assertNotEquals(full.name, short.name)
        assertNotEquals(full.postalCode, short.postalCode)
        assertNotEquals(full.state, short.state)
        assertNotEquals(full.svi, short.svi)
        assertNotEquals(full.updatedAt, short.updatedAt)

        assertNotNull(full.autoContactFrequencyT)
        assertNotNull(full.email)
        assertNotNull(full.phone1)
        assertNotNull(full.phone2)
        assertNotNull(full.plusCode)
        assertNotNull(full.reportedBy)
        assertNotNull(full.what3Words)

        assertNull(short.autoContactFrequencyT)
        assertNull(short.email)
        assertNull(short.phone1)
        assertNull(short.phone2)
        assertNull(short.plusCode)
        assertNull(short.reportedBy)
        assertNull(short.what3Words)
    }

    /**
     * Nullable worksite fields are nullable columns in db
     */
    @Test
    fun syncNewWorksitesShort() = runTest {
        var existingWorksites = listOf(
            testWorksiteShortEntity(1, 1, createdAtA),
        )
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)

        val expected = listOf(existingWorksites[0].copy(id = 1))
        val actual = worksiteDao.streamWorksites(1, 99).first().map { it.entity }
        assertEquals(expected, actual)
    }

    /**
     * Worksite db data coalesces certain nullable columns as expected
     */
    @Test
    fun updateNewWorksitesShort() = runTest {
        // Insert existing
        var existingWorksites = listOf(
            testWorksiteShortEntity(1, 1, createdAtA),
            testWorksiteFullEntity(2, 1, createdAtA),
        )
        existingWorksites = insertWorksites(existingWorksites, previousSyncedAt)
        val existingWorksite = existingWorksites[1]

        // Sync
        val syncingWorksites = listOf(
            // Missing created_at
            testWorksiteShortEntity(1, 1, createdAtB).copy(
                createdAt = null,
                address = "expected-address"
            ),
            testWorksiteShortEntity(2, 1, createdAtB).copy(
                address = "expected-address",
                caseNumber = "expected-case",
                city = "expected-city",
                county = "expected-county",
                favoriteId = existingWorksite.favoriteId!! + 1L,
                keyWorkTypeType = "expected-key-work-type",
                keyWorkTypeStatus = "expected-key-work-status",
                latitude = existingWorksite.latitude + 0.01f,
                longitude = existingWorksite.longitude + 0.01f,
                name = "expected-name",
                postalCode = "expected-code",
                state = "expected-state",
                svi = existingWorksite.svi!! + 0.1f,
                updatedAt = existingWorksite.updatedAt.plus(11.seconds),
            ),
        )
        val syncingWorkTypes = syncingWorksites.map { emptyList<WorkTypeEntity>() }
        // Sync
        val syncedAt = previousSyncedAt.plus(487.seconds)
        worksiteDaoPlus.syncWorksites(1, syncingWorksites, syncingWorkTypes, syncedAt)

        // Assert
        val expected = listOf(
            // Updates certain fields
            syncingWorksites[0].copy(
                id = 1,
                createdAt = createdAtA,
            ),
            // Does not overwrite coalescing columns/fields
            testWorksiteFullEntity(2, 1, createdAtB).copy(
                id = 2,
                address = "expected-address",
                caseNumber = "expected-case",
                city = "expected-city",
                county = "expected-county",
                favoriteId = existingWorksite.favoriteId!! + 1L,
                keyWorkTypeType = "expected-key-work-type",
                keyWorkTypeOrgClaim = null,
                keyWorkTypeStatus = "expected-key-work-status",
                latitude = existingWorksite.latitude + 0.01f,
                longitude = existingWorksite.longitude + 0.01f,
                name = "expected-name",
                postalCode = "expected-code",
                state = "expected-state",
                svi = existingWorksite.svi!! + 0.1f,
                updatedAt = existingWorksite.updatedAt.plus(11.seconds),
            ),
        )
        val actual = worksiteDao.streamWorksites(1, 99).first().map { it.entity }
        assertEquals(expected, actual)
    }

    // TODO Sync existing worksite where the incident changes. Change back as well?
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
    keyWorkTypeOrgClaim = null,
    keyWorkTypeStatus = "",
    latitude = 0.0,
    longitude = 0.0,
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

// Defines all fields setting updated_at to be relative to createdAt
fun testWorksiteFullEntity(
    networkId: Long,
    incidentId: Long,
    createdAt: Instant,
    id: Long = 0
) = WorksiteEntity(
    id = id,
    networkId = networkId,
    incidentId = incidentId,
    address = "123 address st",
    autoContactFrequencyT = "enum.never",
    caseNumber = "case",
    city = "city 123",
    county = "county 123",
    createdAt = createdAt,
    email = "test123@email.com",
    favoriteId = 4134,
    keyWorkTypeType = "key-type-type",
    keyWorkTypeOrgClaim = 652,
    keyWorkTypeStatus = "key-type-status",
    latitude = 414.353,
    longitude = -534.15,
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


// Defines all fields not nullable
fun testWorksiteShortEntity(
    networkId: Long,
    incidentId: Long,
    createdAt: Instant,
    id: Long = 0
) = WorksiteEntity(
    id = id,
    networkId = networkId,
    incidentId = incidentId,
    address = "123 address st short",
    autoContactFrequencyT = null,
    caseNumber = "case short",
    city = "city short 123",
    county = "county short 123",
    createdAt = createdAt,
    email = null,
    favoriteId = 895,
    keyWorkTypeType = "key-short-type",
    keyWorkTypeOrgClaim = null,
    keyWorkTypeStatus = "key-short-status",
    latitude = 856.353,
    longitude = -157.15,
    name = "short worksite",
    phone1 = null,
    phone2 = null,
    plusCode = null,
    postalCode = "83425-shrt",
    reportedBy = null,
    state = "SH",
    svi = 0.548f,
    what3Words = null,
    updatedAt = createdAt.plus(66.seconds),
)