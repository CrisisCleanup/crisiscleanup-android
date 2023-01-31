package com.crisiscleanup.core.network.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkWorksitesShortTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun getWorksitesCount() {
        val contents =
            NetworkAuthResult::class.java.getResource("/getWorksitesCountSuccess.json")
                ?.readText()!!
        val result = json.decodeFromString<NetworkWorksitesCountResult>(contents)

        assertNull(result.errors)
        assertEquals(30, result.count)
    }

    @Test
    fun getWorksitesSuccessResult() {
        val contents =
            NetworkAuthResult::class.java.getResource("/getWorksitesPagedSuccess.json")
                ?.readText()!!
        val result = json.decodeFromString<NetworkWorksitesFullResult>(contents)

        assertNull(result.errors)
        assertEquals(30, result.count)

        // TODO Compare certain cases
        //      Empty work_types (and null key_work_type)
    }

    @Test
    fun getWorksitesResultFail() {
        val contents =
            NetworkAuthResult::class.java.getResource("/expiredTokenResult.json")?.readText()!!
        val result = json.decodeFromString<NetworkWorksitesFullResult>(contents)

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
        val contents =
            NetworkAuthResult::class.java.getResource("/worksitesInvalidIncidentResult.json")
                ?.readText()!!
        val result = json.decodeFromString<NetworkWorksitesFullResult>(contents)

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