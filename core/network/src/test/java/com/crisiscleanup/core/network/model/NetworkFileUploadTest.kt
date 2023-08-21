package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals

class NetworkFileUploadTest {
    @Test
    fun startFileUpload() {
        val actual = TestUtil.decodeResource<NetworkFileUpload>("/startFileUpload.json")

        val expected = NetworkFileUpload(
            id = 19,
            uploadProperties = FileUploadProperties(
                url = "https://crisiscleanup-user-files.s3.amazonaws.com/",
                fields = FileUploadFields(
                    key = "Screenshot 2023-05-22 at 9.08.00 AM-e409ec23517242eaad9eb58017e52702.png",
                    algorithm = "alg",
                    credential = "cred",
                    date = "date",
                    policy = "policy",
                    signature = "sig",
                ),
            ),
        )

        assertEquals(expected, actual)
    }
}
