package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkCaseHistoryTest {
    @Test
    fun getCaseHistoryResult() {
        val result = TestUtil.decodeResource<NetworkCaseHistoryResult>("/getCaseHistory.json")

        assertNull(result.errors)

        val events = result.events
        assertNotNull(events)
        assertEquals(5, events.size)

    }
}