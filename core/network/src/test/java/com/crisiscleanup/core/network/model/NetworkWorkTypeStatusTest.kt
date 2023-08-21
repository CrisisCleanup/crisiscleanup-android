package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkWorkTypeStatusTest {
    @Test
    fun getStatusesSuccessResult() {
        val result =
            TestUtil.decodeResource<NetworkWorkTypeStatusResult>("/getWorkTypeStatuses.json")

        assertNull(result.errors)

        assertEquals(15, result.count)

        val statuses = result.results
        assertNotNull(statuses)
        assertEquals(result.count, statuses.size)
        val expectedStatusFirst = NetworkWorkTypeStatusFull(
            status = "open_unassigned",
            name = "Open, unassigned",
            listOrder = 10,
            primaryState = "open",
        )
        assertEquals(expectedStatusFirst, statuses[0])
        val expectedStatusLast = NetworkWorkTypeStatusFull(
            status = "open_partially-completed",
            name = "Open, partially completed",
            listOrder = 140,
            primaryState = "open",
        )
        assertEquals(expectedStatusLast, statuses[14])
    }
}
