package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.TestCrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.TestUtil.testAppLogger
import com.crisiscleanup.core.database.TestUtil.testAppVersionProvider
import com.crisiscleanup.core.database.TestUtil.testChangeSerializer
import com.crisiscleanup.core.database.TestUtil.testUuidGenerator
import com.crisiscleanup.core.database.WorksiteTestUtil
import com.crisiscleanup.core.database.isNearNow
import com.crisiscleanup.core.database.model.PopulatedIdNetworkId
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.model.data.WorksiteChangeArchiveAction
import com.crisiscleanup.core.model.data.WorksiteSyncResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class WorksiteChangeUpdateSyncTest {
    private lateinit var db: TestCrisisCleanupDatabase
    private lateinit var worksiteChangeDaoPlus: WorksiteChangeDaoPlus
    private lateinit var worksiteDao: WorksiteDao

    private val uuidGenerator = testUuidGenerator()
    private val changeSerializer = testChangeSerializer()
    private val appVersionProvider = testAppVersionProvider()
    private val syncLogger = TestUtil.testSyncLogger()
    private val appLogger = testAppLogger()

    private val testIncidentId = WorksiteTestUtil.testIncidents.last().id

    private val epoch0 = Instant.fromEpochSeconds(0)
    private val now = Clock.System.now()
    private val createdAtA = now.minus(5.days)

    private val rootEntity = WorksiteRootEntity(
        51,
        "sync-uuid-1",
        localModifiedAt = createdAtA,
        epoch0,
        "local-global-uuid-1",
        isLocalModified = true,
        0,
        -1,
        testIncidentId,
    )

    @Before
    fun createDb() {
        db = TestUtil.getTestDatabase()
        worksiteDao = db.worksiteDao()
        worksiteChangeDaoPlus = WorksiteChangeDaoPlus(
            db,
            uuidGenerator,
            changeSerializer,
            appVersionProvider,
            appLogger,
            syncLogger,
        )
    }

    @Before
    fun seedDb() = runTest {
        db.incidentDao().upsertIncidents(WorksiteTestUtil.testIncidents)

        worksiteDao.insertRoot(rootEntity)

        val worksiteChangeDao = db.worksiteChangeDao()
        worksiteChangeDao.insert(testWorksiteChange(51, saveAttempt = 2))

        worksiteDao.insertRoot(
            rootEntity.copy(
                id = 65,
                localGlobalUuid = "local-global-uuid-2",
            )
        )
        val changesB = listOf(
            testWorksiteChange(65),
            testWorksiteChange(65),
            testWorksiteChange(65),
        )
        changesB.forEach { worksiteChangeDao.insert(it) }

        worksiteDao.insertRoot(
            rootEntity.copy(
                id = 77,
                localGlobalUuid = "local-global-uuid-3",
            )
        )
        val changesC = listOf(
            testWorksiteChange(77, archiveAction = WorksiteChangeArchiveAction.Synced),
            testWorksiteChange(77, saveAttempt = 1),
            testWorksiteChange(77, saveAttempt = 2),
            testWorksiteChange(77, saveAttempt = 3),
            testWorksiteChange(77, saveAttempt = 4),
        )
        changesC.forEach { worksiteChangeDao.insert(it) }
    }

    @Test
    fun updateSyncIds() = runTest {
        val worksiteEntity = testWorksiteEntity(
            -1,
            testIncidentId,
            "",
            createdAtA,
            createdAtA,
            51,
        )
        val worksiteDao = db.worksiteDao()
        worksiteDao.insert(worksiteEntity)

        val flagDao = db.worksiteFlagDao()
        flagDao.insertIgnore(
            listOf(
                testFlagEntity(
                    -1,
                    51,
                    createdAtA,
                    "reason-a",
                )
            )
        )

        val noteDao = db.worksiteNoteDao()
        noteDao.insertIgnore(
            listOf(
                testNotesEntity(
                    -1,
                    51,
                    createdAtA,
                    "note-a",
                    localGlobalUuid = "local-global-uuid-2",
                )
            )
        )

        val workTypeDao = db.workTypeDao()
        workTypeDao.insertIgnore(
            listOf(
                testWorkTypeEntity(
                    -1,
                    workType = "work-type-a",
                    worksiteId = 51,
                ),
                testWorkTypeEntity(
                    -1,
                    workType = "work-type-b",
                    worksiteId = 51,
                ),
            )
        )

        val workTypeRequestsDao = db.workTypeTransferRequestDao()
        workTypeRequestsDao.insertReplace(
            listOf(
                testWorkTypeRequestEntity(
                    -1,
                    51,
                    "work-type-a",
                    538,
                ),
                testWorkTypeRequestEntity(
                    34,
                    51,
                    "work-type-b",
                    623,
                ),
                testWorkTypeRequestEntity(
                    58,
                    51,
                    "work-type-b",
                    538,
                ),
            )
        )

        worksiteChangeDaoPlus.updateSyncIds(
            51,
            538,
            WorksiteSyncResult.ChangeIds(
                884,
                flagIdMap = mapOf(1L to 43, 4L to 83),
                noteIdMap = mapOf(9L to 358, 1L to 385),
                workTypeIdMap = mapOf(2L to 837, 83L to 358, 1L to 385),
                workTypeKeyMap = mapOf("work-type-c" to 358, "work-type-b" to 124),
                workTypeRequestIdMap = mapOf(
                    "work-type-a" to 524,
                    "work-type-b" to 529,
                )
            ),
        )

        assertEquals(884L, worksiteDao.getWorksiteNetworkId(51))

        assertEquals(
            listOf(PopulatedIdNetworkId(1, 43)),
            flagDao.getNetworkedIdMap(51),
        )

        assertEquals(
            listOf(PopulatedIdNetworkId(1, 385)),
            noteDao.getNetworkedIdMap(51),
        )

        assertEquals(
            listOf(
                PopulatedIdNetworkId(1, 385),
                PopulatedIdNetworkId(2, 124),
            ),
            workTypeDao.getNetworkedIdMap(51)
                .sortedBy(PopulatedIdNetworkId::id),
        )

        assertEquals(
            listOf(
                PopulatedIdNetworkId(1, 524),
                PopulatedIdNetworkId(2, 34),
                PopulatedIdNetworkId(3, 529),
            ),
            db.testWorkTypeRequestDao().getNetworkedIdMap(51)
                .sortedBy(PopulatedIdNetworkId::id),
        )

        worksiteChangeDaoPlus.updateSyncIds(
            51,
            538,
            WorksiteSyncResult.ChangeIds(
                -1,
                flagIdMap = mapOf(1L to -1),
                noteIdMap = mapOf(9L to -1),
                workTypeIdMap = mapOf(2L to -1L),
                workTypeKeyMap = mapOf("work-type-c" to -1),
                workTypeRequestIdMap = mapOf("work-type-b" to -1),
            ),
        )

        assertEquals(884L, worksiteDao.getWorksiteNetworkId(51))

        assertEquals(
            listOf(PopulatedIdNetworkId(1, 43)),
            flagDao.getNetworkedIdMap(51),
        )

        assertEquals(
            listOf(PopulatedIdNetworkId(1, 385)),
            noteDao.getNetworkedIdMap(51),
        )

        assertEquals(
            listOf(
                PopulatedIdNetworkId(1, 385),
                PopulatedIdNetworkId(2, 124),
            ),
            workTypeDao.getNetworkedIdMap(51)
                .sortedBy(PopulatedIdNetworkId::id),
        )

        assertEquals(
            listOf(
                PopulatedIdNetworkId(1, 524),
                PopulatedIdNetworkId(2, 34),
                PopulatedIdNetworkId(3, 529),
            ),
            db.testWorkTypeRequestDao().getNetworkedIdMap(51)
                .sortedBy(PopulatedIdNetworkId::id),
        )
    }

    @Test
    fun updateSyncChanges_noSyncChanges() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(77, emptyList())

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(77)
        val expected = listOf(
            testWorksiteChange(77, 6, saveAttempt = 1),
            testWorksiteChange(77, 7, saveAttempt = 2),
            testWorksiteChange(77, 8, saveAttempt = 3),
            testWorksiteChange(77, 9, saveAttempt = 4),
        )
        assertEquals(expected, actual)
    }

    // TODO Create custom matcher/assertion
    private fun Instant.assertRecentTime(maxDuration: Duration = 1.seconds) {
        assertTrue(isNearNow(maxDuration))
    }

    @Test
    fun updateSyncChanges_oneFail() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            51, listOf(testChangeResult(1, isFail = true))
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(51)
        val expected = listOf(
            testWorksiteChange(
                51,
                1,
                saveAttempt = 3,
                saveAttemptAt = actual[0].saveAttemptAt,
            )
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_onePartiallySuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            51, listOf(testChangeResult(1, isPartiallySuccessful = true))
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(51)
        val expected = listOf(
            testWorksiteChange(
                51,
                1,
                archiveAction = WorksiteChangeArchiveAction.PartiallySynced,
                saveAttempt = 3,
                saveAttemptAt = actual[0].saveAttemptAt,
            ),
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_oneSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            51, listOf(testChangeResult(1, isSuccessful = true))
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(51)
        assertEquals(emptyList(), actual)
    }

    @Test
    fun updateSyncChanges_manyNoneSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            65,
            listOf(
                testChangeResult(2, isPartiallySuccessful = true),
                testChangeResult(3, isFail = true),
                testChangeResult(4, isFail = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(65)
        val expected = listOf(
            testWorksiteChange(
                65,
                2,
                archiveAction = WorksiteChangeArchiveAction.PartiallySynced,
                saveAttempt = 1,
                saveAttemptAt = actual[0].saveAttemptAt,
            ),
            testWorksiteChange(
                65,
                3,
                saveAttempt = 1,
                saveAttemptAt = actual[1].saveAttemptAt,
            ),
            testWorksiteChange(
                65,
                4,
                saveAttempt = 1,
                saveAttemptAt = actual[2].saveAttemptAt,
            ),
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
        actual[1].saveAttemptAt.assertRecentTime()
        actual[2].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_manyFirstSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            65,
            listOf(
                testChangeResult(2, isSuccessful = true),
                testChangeResult(3, isPartiallySuccessful = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(65)
        val expected = listOf(
            testWorksiteChange(
                65,
                3,
                archiveAction = WorksiteChangeArchiveAction.PartiallySynced,
                saveAttempt = 1,
                saveAttemptAt = actual[0].saveAttemptAt,
            ),
            testWorksiteChange(65, 4),
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_manySecondSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            65,
            listOf(
                testChangeResult(2),
                testChangeResult(3, isSuccessful = true),
                testChangeResult(4, isFail = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(65)
        val expected = listOf(
            testWorksiteChange(
                65,
                4,
                saveAttempt = 1,
                saveAttemptAt = actual[0].saveAttemptAt,
            )
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_manyMiddleSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            77,
            listOf(
                testChangeResult(6, isFail = true),
                testChangeResult(7, isSuccessful = true),
                testChangeResult(8, isPartiallySuccessful = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(77)
        val expected = listOf(
            testWorksiteChange(
                77,
                8,
                archiveAction = WorksiteChangeArchiveAction.PartiallySynced,
                saveAttempt = 4,
                saveAttemptAt = actual[0].saveAttemptAt,
            ),
            testWorksiteChange(77, 9, saveAttempt = 4)
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_manySecondToLastSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            77,
            listOf(
                testChangeResult(6, isSuccessful = true),
                testChangeResult(7, isFail = true),
                testChangeResult(8, isSuccessful = true),
                testChangeResult(9, isFail = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(77)
        val expected = listOf(
            testWorksiteChange(
                77,
                9,
                saveAttempt = 5,
                saveAttemptAt = actual[0].saveAttemptAt,
            )
        )
        assertEquals(expected, actual)
        actual[0].saveAttemptAt.assertRecentTime()
    }

    @Test
    fun updateSyncChanges_manyLastSuccessful() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            77,
            listOf(
                testChangeResult(6, isPartiallySuccessful = true),
                testChangeResult(7, isFail = true),
                testChangeResult(8, isSuccessful = true),
                testChangeResult(9, isSuccessful = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(77)
        assertEquals(emptyList(), actual)
    }

    /**
     * Simulates when changes are added after a sync started before changes are updated
     */
    @Test
    fun updateSyncChanges_insertBeforeUpdatingChanges() = runTest {
        worksiteChangeDaoPlus.updateSyncChanges(
            65,
            listOf(
                testChangeResult(2),
                testChangeResult(3, isSuccessful = true),
            ),
        )

        val actual = db.testWorksiteChangeDao().getEntitiesOrderId(65)
        val expected = listOf(testWorksiteChange(65, 4))
        assertEquals(expected, actual)
    }

    private fun testWorksiteChange(
        worksiteId: Long,
        id: Long = 0,
        createdAt: Instant = createdAtA,
        saveAttempt: Int = 0,
        archiveAction: WorksiteChangeArchiveAction = WorksiteChangeArchiveAction.Pending,
        saveAttemptAt: Instant = epoch0,
    ) = WorksiteChangeEntity(
        id = id,
        appVersion = 1,
        organizationId = 1,
        worksiteId = worksiteId,
        syncUuid = "",
        changeModelVersion = 1,
        changeData = "change-data",
        createdAt = createdAt,
        saveAttempt = saveAttempt,
        archiveAction = archiveAction.literal,
        saveAttemptAt = saveAttemptAt,
    )

    private fun testChangeResult(
        id: Long,
        isSuccessful: Boolean = false,
        isPartiallySuccessful: Boolean = false,
        isFail: Boolean = false,
    ) = WorksiteSyncResult.ChangeResult(
        id = id,
        isSuccessful = isSuccessful,
        isPartiallySuccessful = isPartiallySuccessful,
        isFail = isFail,
    )

    private fun testWorkTypeRequestEntity(
        networkId: Long,
        worksiteId: Long,
        workType: String,
        byOrg: Long = 52,
        reason: String = "reason",
        toOrg: Long = 83,
        createdAt: Instant = now,
        id: Long = 0,
    ) = WorkTypeTransferRequestEntity(
        id = id,
        networkId = networkId,
        worksiteId = worksiteId,
        workType = workType,
        reason = reason,
        byOrg = byOrg,
        toOrg = toOrg,
        createdAt = createdAt,
    )
}