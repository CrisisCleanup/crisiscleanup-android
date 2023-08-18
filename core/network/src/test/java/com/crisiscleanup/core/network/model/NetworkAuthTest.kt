package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkAuthTest {
    @Test
    fun authSuccessResult() {
        val result = TestUtil.decodeResource<NetworkAuthResult>("/authResponseSuccess.json")

        assertNull(result.errors)

        assertEquals(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiJ9.eyJ1c2VybmFtZSI6ImRlbW9AY3Jpc2lzY2xlYW51cC5vcmciLCJpYXQiOjE2NzMzNzQ2NDIsImV4cCI6MTY3MzQ2MTA0MiwianRpIjoiZjI3Y2Y4ZTItYjNkNi00YTJjLTkzMjgtZmVhODQxMjQ5NzU4IiwidXNlcl9pZCI6MTg2MDIsIm9yaWdfaWF0IjoxNjczMzc0NjQyLCJhdWQiOiJodHRwczovL2Rldi5jcmlzaXNjbGVhbnVwLmlvIiwiaXNzIjoiVGVtY2tmdEp0S1RmZzhDcWg4bXBnM29BRkdpVDFURFIifQ.ouHPjdWIAp-92VsyHBA-CDIviqzaWNTbxA_tW137gPQnpbziBjn2infhQjJznh5fX__jqjdtOdAaaonUbPVYHXKDWW-iAEEg6z3FTEPF9G4g-_1r55uhA8sTvdzrzBaFRNhkoYYXHQjP-KKuETJdkKXSiYAPWldK5qFRkhbhEIcRe7fawctxnSo0J7TMC9TLrjDicC7ykcrzEyfhtOKD4FBuE_85op6xuqzr-6i2UKh4grw_TkZ6CzD98LSsBHqIDjQEU0rHgY-8mTEV803P3pVQMU6_0rB4UtWuNetvZafFI9CD2lQtAUyw2LgZEu-J0WNPniAyoR8Tg1P089CKiO2LWfu2E20QDY9qP_hYpnd6w3_wb20U_gf92uNBY-J2MSlqkY4EepmmlKVI01ZT31-8B-4UQRwLG5S2G1gYeOK6UKWGCovotkyrVTs-tGkbz-Z-ROTwiS9RCKLmPtcZna44TH9mKYkY3UN_CUmM0i1qwDLO1FOjPgawISejfIAmrjU0tMDg7KPA7fpA92_UUSRULY-tu6bOLWQctaduQPSV5Iq38cMbmYwDBkZHNmT9xBvrdk_CXRRAOokWht41fM6VGkCjDWdALsnG0OkNRgthQJhceXcj1WIFRRnACXPHwBxq7mfT-XwHL9KbfFtLSBBTr924lR42We161TNyMyI",
            result.accessToken,
        )

        val claims = result.claims!!
        assertEquals(18602, claims.id)
        assertEquals("demo@crisiscleanup.org", claims.email)
        assertEquals("Demo", claims.firstName)
        assertEquals("User", claims.lastName)

        val files = claims.files
        assertEquals(1, files!!.size)
        val firstFile = files[0]
        assertEquals(
            NetworkFile(
                id = 5,
                createdAt = Instant.parse("2020-05-13T05:53:45Z"),
                file = 5,
                fileName = "6645713-b99b0bfba6a04d24879b35538d1c8b9f.jpg",
                url = "https://crisiscleanup-user-files.s3.amazonaws.com/6645713-b99b0bfba6a04d24879b35538d1c8b9f.jpg?AWSAccessKeyId=AKIASU3RMDS2EGFBJH5O&Signature=Ez3PS71Gedweed%2BWZLT0rF%2BU9AY%3D&Expires=1673376442",
                filenameOriginal = "6645713.jpg",
                fileTypeT = "fileTypes.user_profile_picture",
                mimeContentType = "image/jpeg",
            ),
            firstFile,
        )

        val organization = result.organizations!!
        assertEquals(12, organization.id)
        assertEquals("Demo Recovery Organization", organization.name)
        assertEquals(true, organization.isActive)
    }

    @Test
    fun authFailResult() {
        val result = TestUtil.decodeResource<NetworkAuthResult>("/authResponseFail.json")

        assertNull(result.accessToken)
        assertNull(result.claims)
        assertNull(result.organizations)

        assertEquals(1, result.errors?.size)
        val firstError = result.errors!!.first()
        assertEquals(
            NetworkCrisisCleanupApiError(
                "non_field_errors",
                listOf("Unable to log in with provided credentials."),
            ),
            firstError,
        )
    }
}
