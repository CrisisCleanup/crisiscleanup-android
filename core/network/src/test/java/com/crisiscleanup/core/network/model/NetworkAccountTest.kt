package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class NetworkAccountTest {
    @Test
    fun userMeResult() {
        val account = TestUtil.decodeResource<NetworkUserProfile>("/accountResponseSuccess.json")

        assertEquals(18602, account.id)
        assertEquals("demo@crisiscleanup.org", account.email)
        assertEquals("Demo", account.firstName)
        assertEquals("User", account.lastName)
        assertEquals(setOf(153L, 5, 1), account.approvedIncidents)

        val files = account.files
        assertEquals(1, files!!.size)
        val firstFile = files[0]
        assertEquals(
            NetworkFile(
                id = 5,
                createdAt = Instant.parse("2023-06-28T16:23:26Z"),
                file = 87278,
                fileName = "6645713-b99b0bfba6a04d24879b35538d1c8b9f.jpg",
                url = "https://crisiscleanup-user-files.s3.amazonaws.com/6645713-b99b0bfba6a04d24879b35538d1c8b9f.jpg?AWSAccessKeyId=AKIASU3RMDS2EGFBJH5O&Signature=Ez3PS71Gedweed%2BWZLT0rF%2BU9AY%3D&Expires=1673376442",
                fullUrl = "full-url",
                blogUrl = "blog-url",
                largeThumbnailUrl = "large-thumbnail",
                smallThumbnailUrl = "small-thumbnail",
                filenameOriginal = "6645713.jpg",
                fileTypeT = "fileTypes.user_profile_picture",
                mimeContentType = "image/jpeg",
            ),
            firstFile,
        )

        val organization = account.organization
        assertEquals(12, organization.id)
        assertEquals("Demo Recovery Organization", organization.name)
        assertEquals(true, organization.isActive)
    }
}
