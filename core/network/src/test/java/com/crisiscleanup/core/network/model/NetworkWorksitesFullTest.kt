package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkWorksitesFullTest {
    @Test
    fun getWorksitesCount() {
        val result =
            TestUtil.decodeResource<NetworkCountResult>("/getWorksitesCountSuccess.json")

        assertNull(result.errors)
        assertEquals(30, result.count)
    }

    @Test
    fun getWorksitesSuccessResult() {
        val result =
            TestUtil.decodeResource<NetworkWorksitesFullResult>("/getWorksitesPagedSuccess.json")

        assertNull(result.errors)
        assertEquals(30, result.count)

        // TODO Compare certain cases
        //      Empty work_types (and null key_work_type)
    }

    @Test
    fun getWorksites2SuccessResult() {
        val result = TestUtil.decodeResource<NetworkWorksitesFullResult>("/getWorksitesPaged2.json")

        assertNull(result.errors)
        assertEquals(30, result.count)

        // TODO Compare certain cases
        //      Empty work_types (and null key_work_type)
        //      favorite not null
    }

    @Test
    fun getWorksitesResultFail() {
        val result = TestUtil.decodeResource<NetworkWorksitesFullResult>("/expiredTokenResult.json")

        assertNull(result.count)
        assertNull(result.results)

        assertEquals(1, result.errors?.size)
        val firstError = result.errors?.get(0)!!
        assertEquals(
            NetworkCrisisCleanupApiError(
                field = "detail",
                message = listOf("Token has expired."),
            ),
            firstError,
        )
    }

    @Test
    fun invalidIncidentIdResponse() {
        val result =
            TestUtil.decodeResource<NetworkWorksitesFullResult>("/worksitesInvalidIncidentResult.json")

        assertNull(result.count)
        assertNull(result.results)

        assertEquals(1, result.errors?.size)
        val firstError = result.errors?.get(0)!!
        assertEquals(
            NetworkCrisisCleanupApiError(
                field = "incident",
                message = listOf("Select a valid choice. That choice is not one of the available choices."),
            ),
            firstError,
        )
    }
}
