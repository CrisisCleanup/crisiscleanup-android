package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestUtil.testSyncLogger
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.model.WorkTypeEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class WorkTypeDaoTest {
    private lateinit var db: TestCrisisCleanupDatabase

    private lateinit var worksiteDao: WorksiteDao
    private lateinit var worksiteDaoPlus: WorksiteDaoPlus
    private lateinit var workTypeDao: WorkTypeDao
    private lateinit var workTypeDaoPlus: WorkTypeDaoPlus

    private val syncLogger = testSyncLogger()

    private val now = Clock.System.now()
    private val updatedA = now.plus((-9999).seconds)

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteDaoPlus = WorksiteDaoPlus(db, syncLogger)
        workTypeDao = db.workTypeDao()
        workTypeDaoPlus = WorkTypeDaoPlus(db)
    }

    @Before
    fun seedDb() = runTest {
        val incidentDao = db.incidentDao()
        incidentDao.upsertIncidents(WorksiteTestUtil.testIncidents)

        val worksite = testWorksiteEntity(1, 1, "address", updatedA)
        WorksiteTestUtil.insertWorksites(db, now, worksite)
    }

    /**
     * Updates short work type data with full network data
     */
    @Test
    fun syncWorkTypeFullFromShort() = runTest {
        workTypeDaoPlus.syncUpsert(
            listOf(
                testWorkTypeEntity(111),
                testWorkTypeEntity(112, workType = "work-type-b"),
            ),
        )

        val workTypeFull = fullWorkTypeEntity(
            111,
            createdAt = now,
            workType = "work-type-a",
        )
        workTypeDaoPlus.syncUpsert(listOf(workTypeFull))
        val expected = listOf(
            // Update
            workTypeFull.copy(id = 1),
            // Unchanged
            testWorkTypeEntity(112, workType = "work-type-b").copy(id = 2),
        )
        val actual = db.testWorkTypeDao().getEntities(1)
        assertEquals(expected, actual)
    }

    /**
     * Updates full work type data with short network data
     *
     * created_at is not overwritten
     */
    @Test
    fun syncWorkTypeShortFromFull() = runTest {
        val workTypeFull = fullWorkTypeEntity(
            111,
            createdAt = now,
            workType = "work-type-a",
        )
        workTypeDaoPlus.syncUpsert(
            listOf(
                workTypeFull,
                testWorkTypeEntity(112, workType = "work-type-b"),
            ),
        )

        workTypeDaoPlus.syncUpsert(
            listOf(
                testWorkTypeEntity(111, "s", "work-type-a"),
                testWorkTypeEntity(350, "sa", "wa"),
            ),
        )
        val expecteds = listOf(
            // Inserts
            testWorkTypeEntity(350, "sa", "wa").copy(id = 4),
            // Update
            testWorkTypeEntity(111, "s", "work-type-a").copy(
                id = 1,
                createdAt = now,
            ),
            // Unchanged
            testWorkTypeEntity(112, workType = "work-type-b").copy(id = 2),
        )
        val actual = db.testWorkTypeDao().getEntities(1)
        assertEquals(expecteds, actual)
    }
}

internal fun testWorkTypeEntity(
    networkId: Long,
    status: String = "status",
    workType: String = "work-type-a",
    orgClaim: Long? = 201,
    worksiteId: Long = 1,
    createdAt: Instant? = null,
    nextRecurAt: Instant? = null,
    phase: Int? = null,
    recur: String? = null,
    id: Long = 0,
) = WorkTypeEntity(
    id = id,
    networkId = networkId,
    worksiteId = worksiteId,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = nextRecurAt,
    phase = phase,
    recur = recur,
    status = status,
    workType = workType,
)

internal fun fullWorkTypeEntity(
    networkId: Long = 111,
    createdAt: Instant,
    status: String = "status-full",
    workType: String = "work-type-full",
    orgClaim: Long = 4851,
) = testWorkTypeEntity(
    networkId,
    status,
    workType,
    orgClaim,
    1,
    createdAt = createdAt,
    nextRecurAt = createdAt.plus(5413.seconds),
    phase = 53,
    recur = "recur",
)
