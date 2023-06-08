package com.crisiscleanup.core.network.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

private val now = Clock.System.now()

class NewestWorkTypes {

    private val createdAtA = now.minus(16.hours)

    @Test
    fun noWorkTypes() {
        val (newestWorkTypes, newestKeyWorkType) = NetworkWorksiteFull.distinctNewestWorkTypes(
            emptyList(), null
        )
        assertEquals(emptyList(), newestWorkTypes)
        assertNull(newestKeyWorkType)

        val (workTypesShort, keyWorkTypeShort) = NetworkWorksiteShort.distinctNewestWorkTypes(
            emptyList(), null
        )
        assertEquals(emptyList(), workTypesShort)
        assertNull(keyWorkTypeShort)
    }

    @Test
    fun noDuplicateWorkTypes() {
        val keyWorkType = testWorkType(2)
        val workTypes = listOf(
            testWorkType(75),
            testWorkType(1),
            testWorkType(2),
        )
        val (newestWorkTypes, newestKeyWorkType) =
            NetworkWorksiteFull.distinctNewestWorkTypes(workTypes, keyWorkType)

        assertEquals(
            listOf(
                testWorkType(75),
                testWorkType(1),
                testWorkType(2),
            ),
            newestWorkTypes,
        )
        assertEquals(testWorkType(2), newestKeyWorkType)
    }

    @Test
    fun noDuplicateWorkTypes_short() {
        val workTypes = listOf(
            testWorkTypeShort(75),
            testWorkTypeShort(1),
            testWorkTypeShort(2),
        )
        val keyWorkType = NetworkWorksiteFull.KeyWorkTypeShort("work-type-2", null, "status")
        val (newestWorkTypes, newestKeyWorkType) =
            NetworkWorksiteShort.distinctNewestWorkTypes(workTypes, keyWorkType)
        assertEquals(workTypes, newestWorkTypes)
        assertEquals(keyWorkType, newestKeyWorkType)
    }

    @Test
    fun duplicateWorkTypes() {
        val keyWorkType = testWorkType(2, workType = "work-type-m")
        val workTypes = listOf(
            testWorkType(81, workType = "work-type-b"),
            testWorkType(2, workType = "work-type-m"),
            testWorkType(155, workType = "work-type-a"),
            testWorkType(1, workType = "work-type-b"),
            testWorkType(75, workType = "work-type-a"),
            testWorkType(
                52,
                workType = "work-type-m",
                orgClaim = 152,
                status =
                "status-high",
                createdAt = createdAtA
            ),
            testWorkType(21, workType = "work-type-b"),
        )
        val (newestWorkTypes, newestKeyWorkType) =
            NetworkWorksiteFull.distinctNewestWorkTypes(workTypes, keyWorkType)

        assertEquals(
            listOf(
                testWorkType(81, workType = "work-type-b"),
                testWorkType(155, workType = "work-type-a"),
                testWorkType(
                    52,
                    workType = "work-type-m",
                    orgClaim = 152,
                    status = "status-high",
                    createdAt = createdAtA,
                ),
            ),
            newestWorkTypes,
        )
        assertEquals(
            testWorkType(
                52,
                workType = "work-type-m",
                orgClaim = 152,
                status = "status-high",
                createdAt = createdAtA,
            ),
            newestKeyWorkType,
        )
    }

    @Test
    fun duplicateWorkTypes_short() {
        val keyWorkType = NetworkWorksiteFull.KeyWorkTypeShort(
            "work-type-m",
            null,
            "status",
        )
        val workTypes = listOf(
            testWorkTypeShort(81, workType = "work-type-b"),
            testWorkTypeShort(2, workType = "work-type-m"),
            testWorkTypeShort(155, workType = "work-type-a"),
            testWorkTypeShort(1, workType = "work-type-b"),
            testWorkTypeShort(75, workType = "work-type-a"),
            testWorkTypeShort(
                52,
                workType = "work-type-m",
                orgClaim = 158,
                status = "status-high",
            ),
            testWorkTypeShort(21, workType = "work-type-b"),
        )

        val (newestWorkTypes, newestKeyWorkType) =
            NetworkWorksiteShort.distinctNewestWorkTypes(workTypes, keyWorkType)

        assertEquals(
            listOf(
                testWorkTypeShort(81, workType = "work-type-b"),
                testWorkTypeShort(155, workType = "work-type-a"),
                testWorkTypeShort(
                    52,
                    workType = "work-type-m",
                    orgClaim = 158,
                    status = "status-high",
                ),
            ),
            newestWorkTypes,
        )
        assertEquals(
            NetworkWorksiteFull.KeyWorkTypeShort(
                workType = "work-type-m",
                orgClaim = 158,
                status = "status-high",
            ),
            newestKeyWorkType,
        )
    }

    @Test
    fun duplicateWorkTypes_differentKeyWorkType() {
        val keyWorkType = testWorkType(2, workType = "work-type-m")
        val workTypes = listOf(
            testWorkType(81, workType = "work-type-b"),
            testWorkType(2, workType = "work-type-m"),
            testWorkType(155, workType = "work-type-a"),
            testWorkType(1, workType = "work-type-b"),
            testWorkType(75, workType = "work-type-a"),
            testWorkType(
                52,
                workType = "work-type-m",
                orgClaim = 152,
                status =
                "status-high",
                createdAt = createdAtA
            ),
            testWorkType(21, workType = "work-type-b"),
        )

        val (newestWorkTypes, newestKeyWorkType) =
            NetworkWorksiteFull.distinctNewestWorkTypes(workTypes, keyWorkType)

        assertEquals(
            listOf(
                testWorkType(81, workType = "work-type-b"),
                testWorkType(155, workType = "work-type-a"),
                testWorkType(
                    52,
                    workType = "work-type-m",
                    orgClaim = 152,
                    status = "status-high",
                    createdAt = createdAtA,
                ),
            ),
            newestWorkTypes,
        )
        assertEquals(
            testWorkType(
                52,
                workType = "work-type-m",
                orgClaim = 152,
                status = "status-high",
                createdAt = createdAtA,
            ),
            newestKeyWorkType,
        )
    }
}

internal fun testWorkType(
    id: Long,
    createdAt: Instant = now,
    orgClaim: Long? = null,
    nextRecurAt: Instant? = null,
    phase: Int = 0,
    recur: String = "recur",
    status: String = "status",
    workType: String = "work-type-$id",
) = NetworkWorkType(
    id = id,
    createdAt = createdAt,
    orgClaim = orgClaim,
    nextRecurAt = nextRecurAt,
    phase = phase,
    recur = recur,
    status = status,
    workType = workType,
)

internal fun testWorkTypeShort(
    id: Long,
    orgClaim: Long? = null,
    status: String = "status",
    workType: String = "work-type-$id",
) = NetworkWorksiteFull.WorkTypeShort(
    id = id,
    workType = workType,
    orgClaim = orgClaim,
    status = status,
)
