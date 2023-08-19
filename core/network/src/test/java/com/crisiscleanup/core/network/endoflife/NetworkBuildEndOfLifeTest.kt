package com.crisiscleanup.core.network.endoflife

import com.crisiscleanup.core.network.model.TestUtil
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals

class NetworkBuildEndOfLifeTest {
    @Test
    fun networkEndOfLifeData() {
        val result = TestUtil.decodeResource<NetworkBuildEndOfLife>("/earlybirdEndOfLife.json")

        assertEquals(
            NetworkBuildEndOfLife(
                Instant.parse("2023-09-18T18:06:30.081Z"),
                "This app build has expired",
                "A new Crisis Cleanup app is available.\nSearch \"Crisis Cleanup\" on Google Play.\nOr reach out to Crisis Cleanup team for the replacement app.\n\nHappy volunteering! 🙌",
                "https://play.google.com/store/apps/details?id=com.crisiscleanup.prod",
            ),
            result,
        )
    }
}