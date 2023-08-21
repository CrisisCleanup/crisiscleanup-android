package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkWorkTypeRequestTest {
    @Test
    fun getWorkTypeRequests() {
        val result = TestUtil.decodeResource<NetworkWorkTypeRequestResult>("/worksiteRequests.json")

        assertNull(result.errors)

        assertEquals(2, result.count)

        val workTypeRequests = result.results
        assertNotNull(workTypeRequests)
        assertEquals(result.count, workTypeRequests.size)
        val expected = NetworkWorkTypeRequest(
            id = 980,
            workType = NetworkWorkType(
                1508174,
                Instant.parse("2023-04-14T17:56:17Z"),
                4734,
                null,
                4,
                null,
                "closed_no-help-wanted",
                "trees",
            ),
            requestedBy = 31999,
            approvedAt = null,
            rejectedAt = null,
            tokenExpiration = Instant.parse("2023-05-07T14:52:38Z"),
            createdAt = Instant.parse("2023-05-04T14:52:38Z"),
            acceptedRejectedReason = null,
            byOrg = NetworkOrganizationShort(89, "Crisis Cleanup Admin"),
            toOrg = NetworkOrganizationShort(
                4734,
                "test to org",
            ),
            worksite = 252155,
        )
        assertEquals(expected, workTypeRequests[0])
    }
}
