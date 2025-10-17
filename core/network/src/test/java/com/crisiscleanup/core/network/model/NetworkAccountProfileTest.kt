package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class NetworkAccountProfileTest {
    @Test
    fun profileAuthResult() {
        val account = TestUtil.decodeResource<NetworkAccountProfileResult>("/getAccountProfileAuth.json")

        assertEquals(setOf(291L), account.approvedIncidents)
        assertEquals(true, account.hasAcceptedTerms)
        assertEquals(Instant.parse("2025-07-26T19:40:22Z"), account.acceptedTermsTimestamp)
        assertEquals(
            listOf(
                NetworkFile(
                    id = 920,
                    blogUrl = "blog-image",
                    createdAt = Instant.parse("2022-06-17T23:47:21.119619Z"),
                    file = 728,
                    fileTypeT = "fileTypes.user_profile_picture",
                    fileName = "Screenshot 2023-01-09 at 11.31.21 AM-abc.png",
                    filenameOriginal = "Screenshot 2023-01-09 at 11.31.21 AM.png",
                    fullUrl = "full-url",
                    largeThumbnailUrl = "large-thumbnail",
                    mimeContentType = "image/png",
                    smallThumbnailUrl = "small-thumbnail",
                    url = "url-file",
                ),
            ),
            account.files,
        )
        assertEquals(
            NetworkOrganizationShort(
                id = 9,
                name = "Test org",
                isActive = true,
            ),
            account.organization,
        )
        assertEquals(setOf(7), account.activeRoles)
    }

    @Test
    fun profileNoAuthResult() {
        val account = TestUtil.decodeResource<NetworkAccountProfileResult>("/getAccountProfileNoAuth.json", true)

        assertNull(account.approvedIncidents)
        assertNull(account.hasAcceptedTerms)
        assertNull(account.acceptedTermsTimestamp)
        assertEquals(
            listOf(
                NetworkFile(
                    id = 920,
                    blogUrl = "blog-image",
                    createdAt = Instant.parse("2022-06-17T23:47:21.119619Z"),
                    file = 728,
                    fileTypeT = "fileTypes.user_profile_picture",
                    fileName = "Screenshot 2023-01-09 at 11.31.21 AM-abc.png",
                    filenameOriginal = "Screenshot 2023-01-09 at 11.31.21 AM.png",
                    fullUrl = "full-url",
                    largeThumbnailUrl = "large-thumbnail",
                    mimeContentType = "image/png",
                    smallThumbnailUrl = "small-thumbnail",
                    url = "url-file",
                ),
            ),
            account.files,
        )
        assertEquals(
            NetworkOrganizationShort(
                id = 9,
                name = "",
                isActive = null,
            ),
            account.organization,
        )
        assertNull(account.activeRoles)
    }
}
