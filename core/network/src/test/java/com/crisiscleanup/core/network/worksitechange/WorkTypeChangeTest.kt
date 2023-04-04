package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class WorkTypeChangeTest {
    private val emptyChangesResult = Triple(
        emptyList<WorkTypeSnapshot.WorkType>(),
        emptyList<WorkTypeChange>(),
        emptyList<Long>(),
    )

    @Test
    fun workTypeChangeFrom_differentWorkType() {
        val workTypeA =
            WorkTypeSnapshot.WorkType(id = 1, status = "status", workType = "work-type-a")
        val workTypeB =
            WorkTypeSnapshot.WorkType(id = 1, status = "status", workType = "work-type-b")
        assertNull(workTypeA.changeFrom(workTypeB, createdAtA))
    }

    @Test
    fun workTypeChangeFrom_noChanges() {
        val workTypeA = WorkTypeSnapshot.WorkType(
            id = 1,
            workType = "work-type",
            status = "status",
            orgClaim = null,
        )
        val workTypeB = WorkTypeSnapshot.WorkType(
            id = 2,
            workType = "work-type",
            status = "status",
            orgClaim = null,
        )
        assertFalse(workTypeB.changeFrom(workTypeA, createdAtA)!!.hasChange)
    }

    @Test
    fun workTypeChangeFrom_allChanges() {
        val workTypeA = WorkTypeSnapshot.WorkType(
            id = 1,
            workType = "work-type",
            status = "status",
            orgClaim = 45,
        )
        val workTypeB = WorkTypeSnapshot.WorkType(
            id = 2,
            workType = "work-type",
            status = "status-change",
            orgClaim = null,
        )

        val expected = WorkTypeChange(
            -1,
            workTypeB,
            createdAtB,
            isClaimChange = true,
            isStatusChange = true,
        )
        assertEquals(expected, workTypeB.changeFrom(workTypeA, createdAtB))
    }

    @Test
    fun workTypeChanges_noneEmpty() {
        val actual = testNetworkWorksite().getWorkTypeChanges(emptyList(), emptyList(), updatedAtA)
        assertEquals(emptyChangesResult, actual)
    }

    @Test
    fun workTypeChanges_noMatchingDelete() {
        val start = listOf(testWorkTypeSnapshot("work-type", "status"))
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    326,
                    status = "status",
                    workType = "work-type-a",
                )
            )
        )
        val actual = worksite.getWorkTypeChanges(start, emptyList(), updatedAtA)
        assertEquals(emptyChangesResult, actual)
    }

    @Test
    fun workTypeChanges_noChanges() {
        val start = listOf(testWorkTypeSnapshot("work-type-a", "status-b"))
        val change = listOf(testWorkTypeSnapshot("work-type-a", "status-b"))
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    326,
                    status = "status",
                    workType = "work-type-a",
                )
            )
        )
        val actual = worksite.getWorkTypeChanges(start, change, updatedAtA)
        assertEquals(emptyChangesResult, actual)
    }

    @Test
    fun workTypeChanges_notChangedFromExisting() {
        val start = listOf(testWorkTypeSnapshot("work-type-a", "status-b"))
        val change = listOf(testWorkTypeSnapshot("work-type-a", "status"))
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    326,
                    status = "status",
                    workType = "work-type-a",
                )
            )
        )
        val actual = worksite.getWorkTypeChanges(start, change, updatedAtA)
        assertEquals(emptyChangesResult, actual)
    }

    @Test
    fun workTypeChanges_delete() {
        val start = listOf(testWorkTypeSnapshot("work-type-a", "status-b"))
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    326,
                    status = "status",
                    workType = "work-type-a",
                ),
                NetworkWorksiteFull.WorkType(
                    81,
                    status = "status",
                    workType = "work-type-b",
                ),
            )
        )
        val actual = worksite.getWorkTypeChanges(start, emptyList(), updatedAtA)
        assertEquals(emptyChangesResult.copy(third = listOf(326L)), actual)
    }

    @Test
    fun workTypeChanges_new() {
        val nextRecurAt = Clock.System.now().plus(10.days)
        val change = listOf(testWorkTypeSnapshot("work-type-a", "status-b"))
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    81,
                    createdAtA,
                    status = "status",
                    workType = "work-type-b",
                    orgClaim = null,
                    nextRecurAt = nextRecurAt,
                    phase = 2,
                    recur = "recur",
                ),
            )
        )
        val actual = worksite.getWorkTypeChanges(emptyList(), change, updatedAtA)
        val expectedChanges = listOf(
            WorkTypeSnapshot.WorkType(
                id = 53,
                createdAt = null,
                orgClaim = null,
                nextRecurAt = null,
                phase = null,
                recur = null,
                status = "status-b",
                workType = "work-type-a",
            ),
        )
        assertEquals(emptyChangesResult.copy(first = expectedChanges), actual)
    }

    @Test
    fun workTypeChanges_newChange() {
        val nextRecurAt = Clock.System.now().plus(10.days)
        val change = listOf(testWorkTypeSnapshot("work-type-b", "status-b"))
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    81,
                    createdAtA,
                    status = "status",
                    workType = "work-type-b",
                    orgClaim = null,
                    nextRecurAt = nextRecurAt,
                    phase = 2,
                    recur = "recur",
                ),
            )
        )
        val actual = worksite.getWorkTypeChanges(emptyList(), change, updatedAtA)
        val expectedChanges = listOf(
            WorkTypeChange(
                81,
                WorkTypeSnapshot.WorkType(
                    id = 81,
                    createdAt = createdAtA,
                    orgClaim = null,
                    nextRecurAt = nextRecurAt,
                    phase = 2,
                    recur = "recur",
                    status = "status-b",
                    workType = "work-type-b",
                ),
                updatedAtA,
                isClaimChange = false,
                isStatusChange = true,
            )
        )
        assertEquals(emptyChangesResult.copy(second = expectedChanges), actual)
    }

    @Test
    fun workTypeChanges_changing() {
        val nextRecurAt = Clock.System.now().plus(10.days)
        val start = listOf(
            testWorkTypeSnapshot("work-type-a", "status-a"),
            testWorkTypeSnapshot("work-type-b", "status-b"),
            testWorkTypeSnapshot("work-type-c", "status-c", orgClaim = 48),
            testWorkTypeSnapshot("work-type-d", "status-d"),
        )
        val change = listOf(
            testWorkTypeSnapshot("work-type-a", "status-a-change"),
            testWorkTypeSnapshot("work-type-b", "status-b", orgClaim = 456),
            testWorkTypeSnapshot("work-type-c", "status-c"),
            testWorkTypeSnapshot("work-type-d", "status-d", orgClaim = 89),
        )
        val worksite = testNetworkWorksite(
            workTypes = listOf(
                NetworkWorksiteFull.WorkType(
                    81,
                    createdAtA,
                    status = "status",
                    workType = "work-type-b",
                    orgClaim = null,
                    nextRecurAt = nextRecurAt,
                    phase = 2,
                    recur = "recur",
                ),
                NetworkWorksiteFull.WorkType(
                    82,
                    createdAtB,
                    status = "status",
                    workType = "work-type-c",
                    orgClaim = null,
                    phase = 3,
                ),
                NetworkWorksiteFull.WorkType(
                    91,
                    createdAtA,
                    status = "status-d",
                    workType = "work-type-d",
                    orgClaim = 54,
                    nextRecurAt = nextRecurAt,
                    phase = 1,
                    recur = "recur",
                ),
                NetworkWorksiteFull.WorkType(
                    99,
                    createdAtB,
                    status = "status-a-change",
                    workType = "work-type-a",
                    orgClaim = 471,
                    phase = 4,
                ),
            )
        )
        val actual = worksite.getWorkTypeChanges(start, change, updatedAtA)
        val expectedChanges = listOf(
            WorkTypeChange(
                99,
                WorkTypeSnapshot.WorkType(
                    id = 99,
                    createdAt = createdAtB,
                    orgClaim = null,
                    phase = 4,
                    status = "status-a-change",
                    workType = "work-type-a",
                ),
                updatedAtA,
                isClaimChange = true,
                isStatusChange = false,
            ),
            WorkTypeChange(
                81,
                WorkTypeSnapshot.WorkType(
                    id = 81,
                    createdAt = createdAtA,
                    orgClaim = 456,
                    nextRecurAt = nextRecurAt,
                    phase = 2,
                    recur = "recur",
                    status = "status-b",
                    workType = "work-type-b",
                ),
                updatedAtA,
                isClaimChange = true,
                isStatusChange = true,
            ),
            WorkTypeChange(
                82,
                WorkTypeSnapshot.WorkType(
                    id = 82,
                    createdAt = createdAtB,
                    orgClaim = null,
                    phase = 3,
                    status = "status-c",
                    workType = "work-type-c",
                ),
                updatedAtA,
                isClaimChange = false,
                isStatusChange = true,
            ),
            WorkTypeChange(
                91,
                WorkTypeSnapshot.WorkType(
                    id = 91,
                    createdAt = createdAtA,
                    orgClaim = 89,
                    nextRecurAt = nextRecurAt,
                    phase = 1,
                    recur = "recur",
                    status = "status-d",
                    workType = "work-type-d",
                ),
                updatedAtA,
                isClaimChange = true,
                isStatusChange = false,
            ),
        )
        assertEquals(emptyChangesResult.copy(second = expectedChanges), actual)
    }

    fun workTypeChanges_complex() {
        // TODO Cover all cases especially any not covered above
    }
}

private fun testWorkTypeSnapshot(
    workType: String = "work-type",
    status: String = "status",
    orgClaim: Long? = null,
    createdAt: Instant? = null,
    id: Long = 53,
    localId: Long = 59,
) = WorkTypeSnapshot(
    localId = localId,
    workType = WorkTypeSnapshot.WorkType(
        id = id,
        createdAt = createdAt,
        orgClaim = orgClaim,
        status = status,
        workType = workType,
    ),
)