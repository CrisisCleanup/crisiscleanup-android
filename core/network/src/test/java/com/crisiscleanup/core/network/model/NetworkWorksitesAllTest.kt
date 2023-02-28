package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkWorksitesAllTest {
    @Test
    fun getWorksitesSuccessResult() {
        val result =
            TestUtil.decodeResource<NetworkWorksitesShortResult>("/getWorksitesAllSuccess.json")

        assertNull(result.errors)
        assertEquals(30, result.count)

        // TODO Compare certain cases
        //      Empty work_types (and null key_work_type)
        //      Flags exist
    }

    @Test
    fun getWorksitesResultFail() {
        val result =
            TestUtil.decodeResource<NetworkWorksitesShortResult>("/expiredTokenResult.json")

        assertNull(result.count)
        assertNull(result.results)

        assertEquals(1, result.errors?.size)
        val firstError = result.errors?.get(0)!!
        assertEquals(
            NetworkCrisisCleanupApiError(
                field = "detail",
                message = listOf("Token has expired.")
            ),
            firstError
        )
    }

    @Test
    fun invalidIncidentIdResponse() {
        val result =
            TestUtil.decodeResource<NetworkWorksitesShortResult>("/worksitesInvalidIncidentResult.json")

        assertNull(result.count)
        assertNull(result.results)

        assertEquals(1, result.errors?.size)
        val firstError = result.errors?.get(0)!!
        assertEquals(
            NetworkCrisisCleanupApiError(
                field = "incident",
                message = listOf("Select a valid choice. That choice is not one of the available choices.")
            ),
            firstError
        )
    }
}