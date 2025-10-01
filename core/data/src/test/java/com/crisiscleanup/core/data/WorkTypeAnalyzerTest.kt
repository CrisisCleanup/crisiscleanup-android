package com.crisiscleanup.core.data

import com.crisiscleanup.core.database.dao.WorksiteChangeDao
import com.crisiscleanup.core.database.model.PopulatedWorksiteChange
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.network.worksitechange.CoreSnapshot
import com.crisiscleanup.core.network.worksitechange.WorkTypeSnapshot
import com.crisiscleanup.core.network.worksitechange.WorksiteChange
import com.crisiscleanup.core.network.worksitechange.WorksiteSnapshot
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkTypeAnalyzerTest {
    @MockK
    private lateinit var worksiteChangeDao: WorksiteChangeDao

    private lateinit var workTypeAnalyzer: WorkTypeAnalyzer

    private val json = Json {}

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        workTypeAnalyzer = WorksiteChangeWorkTypeAnalyzer(worksiteChangeDao)
    }

    private fun makeWorksiteChange(
        snapshotStart: WorksiteSnapshot?,
        snapshotChange: WorksiteSnapshot,
        worksiteId: Long,
    ) = PopulatedWorksiteChange(
        WorksiteChangeEntity(
            id = 0,
            appVersion = 0,
            organizationId = 0,
            worksiteId = worksiteId,
            syncUuid = "",
            changeModelVersion = 0,
            changeData = json.encodeToString(
                WorksiteChange(
                    start = snapshotStart,
                    change = snapshotChange,
                ),
            ),
            createdAt = Clock.System.now(),
            saveAttempt = 0,
            archiveAction = "",
        ),
    )

    private fun worksiteChange10(
        incidentId: Long = 152,
        worksiteId: Long = 342,
        orgId: Long = 52,
        networkWorksiteId: Long = 40,
    ) = makeWorksiteChange(
        snapshotStart = makeWorksiteSnapshot(
            incidentId = incidentId,
            networkWorksiteId = networkWorksiteId,
            workTypes = listOf(
                makeWorkTypeSnapshot(1),
            ),
        ),
        snapshotChange = makeWorksiteSnapshot(
            incidentId = incidentId,
            workTypes = listOf(
                makeWorkTypeSnapshot(1, WorkTypeStatus.ClosedOutOfScope, orgId),
            ),
        ),
        worksiteId,
    )

    private fun worksiteChangeN10(
        incidentId: Long = 152,
        worksiteId: Long = 343,
        orgId: Long = 52,
        networkWorksiteId: Long = 41,
    ) = makeWorksiteChange(
        snapshotStart = makeWorksiteSnapshot(
            incidentId = incidentId,
            networkWorksiteId = networkWorksiteId,
            workTypes = listOf(
                makeWorkTypeSnapshot(1, orgId = orgId),
            ),
        ),
        snapshotChange = makeWorksiteSnapshot(
            incidentId = incidentId,
            workTypes = listOf(
                makeWorkTypeSnapshot(1),
            ),
        ),
        worksiteId,
    )

    private fun worksiteChange21(
        incidentId: Long = 152,
        worksiteId: Long = 344,
        orgId: Long = 52,
        networkWorksiteId: Long = 42,
    ) = makeWorksiteChange(
        snapshotStart = makeWorksiteSnapshot(
            incidentId = incidentId,
            networkWorksiteId = networkWorksiteId,
            workTypes = listOf(
                makeWorkTypeSnapshot(2, WorkTypeStatus.ClosedOutOfScope),
                makeWorkTypeSnapshot(3),
                makeWorkTypeSnapshot(4),
            ),
        ),
        snapshotChange = makeWorksiteSnapshot(
            incidentId = incidentId,
            workTypes = listOf(
                makeWorkTypeSnapshot(2, WorkTypeStatus.ClosedRejected),
                makeWorkTypeSnapshot(3, WorkTypeStatus.ClosedDuplicate, orgId),
                makeWorkTypeSnapshot(4, orgId = orgId),
            ),
        ),
        worksiteId,
    )

    private fun worksiteChangeN01(
        incidentId: Long = 152,
        worksiteId: Long = 345,
        orgId: Long = 52,
        networkWorksiteId: Long = 43,
    ) = makeWorksiteChange(
        snapshotStart = makeWorksiteSnapshot(
            incidentId = incidentId,
            networkWorksiteId = networkWorksiteId,
            workTypes = listOf(
                makeWorkTypeSnapshot(1, WorkTypeStatus.ClosedCompleted, orgId),
            ),
        ),
        snapshotChange = makeWorksiteSnapshot(
            incidentId = incidentId,
            workTypes = listOf(
                makeWorkTypeSnapshot(1, orgId = orgId),
            ),
        ),
        worksiteId,
    )

    @Test
    fun separateChanges() = runTest {
        var dbChangeCounter = 0
        val dbChanges = listOf(
            worksiteChange10(),
            worksiteChangeN10(),
            worksiteChange21(),
            worksiteChangeN01(),
        )
        every { worksiteChangeDao.getOrgChanges(52) } answers {
            listOf(dbChanges[dbChangeCounter++])
        }

        val expectedCounts = listOf(
            ClaimCloseCounts(1, 1),
            ClaimCloseCounts(-1, 0),
            ClaimCloseCounts(2, 1),
            ClaimCloseCounts(0, -1),
        )
        for (expected in expectedCounts) {
            val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
                52,
                152,
                emptySet(),
            )
            assertEquals(expected, actual)
        }
    }

    @Test
    fun combinedChanges() = runTest {
        every { worksiteChangeDao.getOrgChanges(52) } returns listOf(
            worksiteChange10(),
            worksiteChangeN10(),
            worksiteChange21(),
        )

        val expected = ClaimCloseCounts(2, 2)
        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            52,
            152,
            emptySet(),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun localWorksiteChanges() = runTest {
        every { worksiteChangeDao.getOrgChanges(52) } returns listOf(
            worksiteChange10(worksiteId = 11),
            worksiteChangeN10(worksiteId = 110),
            worksiteChange21(worksiteId = 21),
        )

        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            52,
            152,
            setOf(11, 21, 110),
        )

        assertEquals(ClaimCloseCounts(0, 0), actual)
    }

    @Test
    fun noNetworkWorksiteId() = runTest {
        every { worksiteChangeDao.getOrgChanges(52) } returns listOf(
            worksiteChange10(networkWorksiteId = 0),
            worksiteChangeN10(networkWorksiteId = 0),
            worksiteChange21(networkWorksiteId = 0),
        )

        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            52,
            152,
            emptySet(),
        )

        assertEquals(ClaimCloseCounts(0, 0), actual)
    }

    @Test
    fun differentIncident() = runTest {
        every { worksiteChangeDao.getOrgChanges(52) } returns listOf(
            worksiteChange10(incidentId = 52),
            worksiteChangeN10(),
            worksiteChange21(),
        )

        val expected = ClaimCloseCounts(1, 1)
        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            52,
            152,
            emptySet(),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun noDistinguishingClaimChanges() = runTest {
        val orgId = 42L
        val incidentId = 152L
        val worksiteChange = makeWorksiteChange(
            snapshotStart = makeWorksiteSnapshot(
                incidentId = incidentId,
                workTypes = listOf(
                    makeWorkTypeSnapshot(2, WorkTypeStatus.OpenUnresponsive),
                    makeWorkTypeSnapshot(3, WorkTypeStatus.ClosedIncomplete, orgId),
                ),
            ),
            snapshotChange = makeWorksiteSnapshot(
                incidentId = incidentId,
                workTypes = listOf(
                    makeWorkTypeSnapshot(2, WorkTypeStatus.NeedUnfilled),
                    makeWorkTypeSnapshot(3, WorkTypeStatus.ClosedDoneByOthers, orgId),
                ),
            ),
            152,
        )

        every { worksiteChangeDao.getOrgChanges(orgId) } returns listOf(worksiteChange)

        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            orgId,
            incidentId,
            emptySet(),
        )

        assertEquals(ClaimCloseCounts(0, 0), actual)
    }

    @Test
    fun noDistinguishingCloseChange() = runTest {
        val orgId = 42L
        val incidentId = 152L
        val worksiteChange = makeWorksiteChange(
            snapshotStart = makeWorksiteSnapshot(
                incidentId = incidentId,
                workTypes = listOf(
                    makeWorkTypeSnapshot(2, WorkTypeStatus.ClosedOutOfScope),
                    makeWorkTypeSnapshot(3, WorkTypeStatus.ClosedIncomplete),
                    makeWorkTypeSnapshot(4, orgId = orgId),
                ),
            ),
            snapshotChange = makeWorksiteSnapshot(
                incidentId = incidentId,
                workTypes = listOf(
                    // No change in claim
                    makeWorkTypeSnapshot(2, WorkTypeStatus.OpenUnresponsive),
                    // Claimed not closed
                    makeWorkTypeSnapshot(3, WorkTypeStatus.OpenAssigned, orgId),
                    // Unclaimed closed
                    makeWorkTypeSnapshot(4, WorkTypeStatus.ClosedDoneByOthers),
                ),
            ),
            152,
        )

        every { worksiteChangeDao.getOrgChanges(orgId) } returns listOf(worksiteChange)

        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            orgId,
            incidentId,
            emptySet(),
        )

        assertEquals(ClaimCloseCounts(0, 0), actual)
    }

    @Test
    fun multipleCommits() = runTest {
        val orgId = 42L
        val worksiteId = 88L
        val incidentId = 152L

        every { worksiteChangeDao.getOrgChanges(orgId) } returns listOf(
            worksiteChangeN10(worksiteId = worksiteId),
            worksiteChange10(worksiteId = worksiteId),
            worksiteChange21(worksiteId = worksiteId),
            worksiteChangeN01(worksiteId = worksiteId),
        )

        val expected = ClaimCloseCounts(0, 0)
        val actual = workTypeAnalyzer.countUnsyncedClaimCloseWork(
            orgId,
            incidentId,
            emptySet(),
        )
        assertEquals(expected, actual)
    }
}

private fun makeWorksiteSnapshot(
    incidentId: Long = 152,
    networkWorksiteId: Long = 6252,
    workTypes: List<WorkTypeSnapshot> = emptyList(),
) = WorksiteSnapshot(
    makeCoreSnapshot(
        incidentId = incidentId,
        networkId = networkWorksiteId,
    ),
    emptyList(),
    emptyList(),
    workTypes,
)

private fun makeCoreSnapshot(
    incidentId: Long = 152,
    networkId: Long = 6252,
) = emptyCoreSnapshot.copy(
    incidentId = incidentId,
    networkId = networkId,
)

private val emptyCoreSnapshot = CoreSnapshot(
    id = 0,
    address = "",
    autoContactFrequencyT = "",
    caseNumber = "",
    city = "",
    county = "",
    createdAt = null,
    email = null,
    favoriteId = null,
    formData = emptyMap(),
    incidentId = 0,
    keyWorkTypeId = null,
    latitude = 0.0,
    longitude = 0.0,
    name = "",
    networkId = 0,
    phone1 = "",
    phone2 = "",
    postalCode = "",
    reportedBy = null,
    state = "",
    svi = null,
    updatedAt = null,
    isAssignedToOrgMember = false,
)

private fun makeWorkTypeSnapshot(
    localId: Long,
    status: WorkTypeStatus = WorkTypeStatus.OpenUnassigned,
    orgId: Long? = null,
) = emptyWorkTypeSnapshot.copy(
    localId = localId,
    workType = emptyWorkTypeSnapshot.workType.copy(
        orgClaim = orgId,
        status = status.literal,
    ),
)

private val emptyWorkTypeSnapshot = WorkTypeSnapshot(
    localId = 0,
    workType = WorkTypeSnapshot.WorkType(
        id = 0,
        orgClaim = null,
        status = "",
        workType = "",
    ),
)
