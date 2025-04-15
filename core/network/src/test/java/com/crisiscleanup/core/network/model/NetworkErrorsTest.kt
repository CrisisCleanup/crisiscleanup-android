package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals

class NetworkErrorsTest {
    @Test
    fun parseNetworkErrors() {
        val errors = TestUtil.decodeResource<NetworkErrors>("/invalidPhoneError.json")

        assertEquals(
            NetworkErrors(
                listOf(
                    NetworkCrisisCleanupApiError(
                        field = "non_field_errors",
                        message = listOf("info.invalid_phone_number"),
                    ),
                ),
            ),
            errors,
        )
    }
}
