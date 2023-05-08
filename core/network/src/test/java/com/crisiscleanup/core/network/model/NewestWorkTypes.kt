package com.crisiscleanup.core.network.model

import com.crisiscleanup.core.network.worksitechange.testNetworkWorksite
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

private val now = Clock.System.now()

class NewestWorkTypes {

    private val createdAtA = now.minus(16.hours)
    private val createdAtB = createdAtA.plus(7.hours)

    @Test
    fun noWorkTypes() {
        val worksite = testNetworkWorksite()
        assertEquals(emptyList(), worksite.newestWorkTypes)
        assertNull(worksite.newestKeyWorkType)

        val worksiteShort = testNetworkWorksiteShort()
        assertEquals(emptyList(), worksiteShort.newestWorkTypes)
        assertNull(worksiteShort.newestKeyWorkType)
    }

    @Test
    fun noDuplicateWorkTypes() {
        val worksite = testNetworkWorksite(
            keyWorkType = testWorkType(2),
            workTypes = listOf(
                testWorkType(75),
                testWorkType(1),
                testWorkType(2),
            )
        )
        assertEquals(
            listOf(
                testWorkType(75),
                testWorkType(1),
                testWorkType(2),
            ),
            worksite.newestWorkTypes,
        )
        assertEquals(testWorkType(2), worksite.newestKeyWorkType)
    }

    @Test
    fun noDuplicateWorkTypes_short() {
        val worksiteShort = testNetworkWorksiteShort(
            keyWorkType = NetworkWorksiteFull.KeyWorkTypeShort("work-type-2", null, "status"),
            workTypes = listOf(
                testWorkTypeShort(75),
                testWorkTypeShort(1),
                testWorkTypeShort(2),
            )
        )
        assertEquals(
            listOf(
                testWorkTypeShort(75),
                testWorkTypeShort(1),
                testWorkTypeShort(2),
            ),
            worksiteShort.newestWorkTypes,
        )
        assertEquals(
            NetworkWorksiteFull.KeyWorkTypeShort("work-type-2", null, "status"),
            worksiteShort.newestKeyWorkType,
        )
    }

    @Test
    fun duplicateWorkTypes() {
        val worksite = testNetworkWorksite(
            keyWorkType = testWorkType(2, workType = "work-type-m"),
            workTypes = listOf(
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
        )
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
            worksite.newestWorkTypes,
        )
        assertEquals(
            testWorkType(
                52,
                workType = "work-type-m",
                orgClaim = 152,
                status = "status-high",
                createdAt = createdAtA,
            ),
            worksite.newestKeyWorkType,
        )
    }

    @Test
    fun duplicateWorkTypes_short() {
        val worksiteShort = testNetworkWorksiteShort(
            keyWorkType = NetworkWorksiteFull.KeyWorkTypeShort(
                "work-type-m",
                null,
                "status",
            ),
            workTypes = listOf(
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
        )
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
            worksiteShort.newestWorkTypes,
        )
        assertEquals(
            NetworkWorksiteFull.KeyWorkTypeShort(
                workType = "work-type-m",
                orgClaim = 158,
                status = "status-high",
            ),
            worksiteShort.newestKeyWorkType,
        )
    }

    @Test
    fun duplicateWorkTypes_differentKeyWorkType() {
        val worksite = testNetworkWorksite(
            keyWorkType = testWorkType(2, workType = "work-type-m"),
            workTypes = listOf(
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
        )
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
            worksite.newestWorkTypes,
        )
        assertEquals(
            testWorkType(
                52,
                workType = "work-type-m",
                orgClaim = 152,
                status = "status-high",
                createdAt = createdAtA,
            ),
            worksite.newestKeyWorkType,
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

internal fun testNetworkWorksiteShort(
    id: Long = 0,
    address: String = "address",
    caseNumber: String = "case-number",
    city: String = "city",
    county: String = "county",
    createdAt: Instant = now,
    flags: List<NetworkWorksiteFull.FlagShort> = emptyList(),
    keyWorkType: NetworkWorksiteFull.KeyWorkTypeShort? = null,
    location: NetworkWorksiteFull.Location = NetworkWorksiteFull.Location(
        "Point",
        listOf(0.0, 0.0)
    ),
    name: String = "name",
    postalCode: String = "postal-code",
    state: String = "state",
    svi: Float = 0.5f,
    updatedAt: Instant = now,
    workTypes: List<NetworkWorksiteFull.WorkTypeShort> = emptyList(),
) = NetworkWorksiteShort(
    id = id,
    address = address,
    caseNumber = caseNumber,
    city = city,
    county = county,
    createdAt = createdAt,
    flags = flags,
    keyWorkType = keyWorkType,
    location = location,
    name = name,
    postalCode = postalCode,
    state = state,
    svi = svi,
    updatedAt = updatedAt,
    workTypes = workTypes,
)