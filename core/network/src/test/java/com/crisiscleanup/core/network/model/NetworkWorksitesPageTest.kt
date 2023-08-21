package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkWorksitesPageTest {
    @Test
    fun getWorksitesSuccessResult() {
        val result =
            TestUtil.decodeResource<NetworkWorksitesPageResult>("/worksitesPageSuccess.json")

        assertNull(result.errors)
        assertEquals(146, result.count)
        assertEquals(14, result.results?.size)
    }

    @Test
    fun getWorksitesResultFail() {
        val result = TestUtil.decodeResource<NetworkWorksitesPageResult>("/expiredTokenResult.json")

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
}
