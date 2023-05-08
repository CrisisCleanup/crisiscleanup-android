package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private val now = Clock.System.now()

class WorkTypeTransferRequestDaoTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var requestDao: WorkTypeTransferRequestDao
    private lateinit var requestDaoPlus: WorkTypeTransferRequestDaoPlus

    private val syncLogger = TestUtil.testSyncLogger()

    private val createdAtA = now.plus((-9999).seconds)
    private val updatedAtA = createdAtA.plus((451).seconds)

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db, syncLogger)
        requestDao = db.workTypeTransferRequestDao()
        requestDaoPlus = WorkTypeTransferRequestDaoPlus(db)
    }

    @Before
    fun seedDb() = runTest {
        val incidentDao = db.incidentDao()
        incidentDao.upsertIncidents(WorksiteTestUtil.testIncidents)

        val worksite = testWorksiteEntity(1, 1, "address", createdAtA)
        WorksiteTestUtil.insertWorksites(db, now, worksite)
    }

    @Test
    fun syncRequests() = runTest {
        val existingRequests = listOf(
            testWorkTypeTransferRequestEntity("work-type-a", 331, createdAtA, networkId = 851),
            testWorkTypeTransferRequestEntity("work-type-b", 331, createdAtA),
            testWorkTypeTransferRequestEntity("work-type-c", 331, createdAtA, networkId = 93),
            testWorkTypeTransferRequestEntity("work-type-d", 331, createdAtA),
        )
        existingRequests.forEach { requestDao.insertIgnoreRequest(it) }

        val newRequests = listOf(
            // Update
            testWorkTypeTransferRequestEntity(
                "work-type-b",
                331,
                updatedAtA,
                "reason-updated",
                toOrg = 513,
                networkId = 593,
                rejectedAt = updatedAtA,
                approvedRejectedReason = "rejected"
            ),
            // Update
            testWorkTypeTransferRequestEntity(
                "work-type-c",
                331,
                updatedAtA,
                "reason-updated",
                toOrg = 513,
                networkId = 93,
                approvedAt = updatedAtA,
                approvedRejectedReason = "approved"
            ),
            // New, different byOrg
            testWorkTypeTransferRequestEntity(
                "work-type-c",
                129,
                updatedAtA,
                "reason-new",
                toOrg = 513,
                // Production data should have distinct network IDs. For testing purposes.
                networkId = 93,
                approvedAt = updatedAtA,
            ),
            // New work type
            testWorkTypeTransferRequestEntity(
                "work-type-e",
                331,
                updatedAtA,
                toOrg = 591,
            ),
        )
        requestDaoPlus.syncUpsert(newRequests)

        val expected = mutableListOf(
            testWorkTypeTransferRequestEntity("work-type-d", 331, createdAtA, id = 4),
        ).apply {
            val entityIds = listOf(2L, 3, 7, 8)
            val reasons = listOf("reason", "reason", "reason-new", "reason")
            addAll(newRequests.mapIndexed { index, entity ->
                entity.copy(
                    id = entityIds[index],
                    reason = reasons[index],
                )
            })
            sortBy(WorkTypeTransferRequestEntity::id)
        }
        val actual = db.testWorkTypeRequestDao().getEntities(1)
            .sortedBy(WorkTypeTransferRequestEntity::id)
        assertEquals(expected, actual)
    }
}

private fun testWorkTypeTransferRequestEntity(
    workType: String,
    byOrg: Long,
    createdAt: Instant = now,
    reason: String = "reason",
    worksiteId: Long = 1,
    id: Long = 0,
    networkId: Long = -1,
    toOrg: Long = 0,
    approvedAt: Instant? = null,
    rejectedAt: Instant? = null,
    approvedRejectedReason: String = "",
) = WorkTypeTransferRequestEntity(
    id = id,
    networkId = networkId,
    worksiteId = worksiteId,
    workType = workType,
    reason = reason,
    byOrg = byOrg,
    toOrg = toOrg,
    createdAt = createdAt,
    approvedAt = approvedAt,
    rejectedAt = rejectedAt,
    approvedRejectedReason = approvedRejectedReason,
)