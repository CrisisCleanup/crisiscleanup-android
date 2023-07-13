package com.crisiscleanup.core.datastore

class AccountInfoDataSourceTest {
    // TODO Rewrite tests
//    private lateinit var subject: AccountInfoDataSource
//
//    @get:Rule
//    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()
//
//    @Before
//    fun setup() {
//        subject = AccountInfoDataSource(
//            tmpFolder.testAccountInfoDataStore()
//        )
//    }
//
//    @Test
//    fun unauthenticatedAccountByDefault() = runTest {
//        assertTrue { subject.accountData.first().accessToken.isEmpty() }
//    }
//
//    @Test
//    fun setAccount_clearAccount() = runTest {
//        subject.setAccount(
//            523,
//            "access-token",
//            "email",
//            "first",
//            "last",
//            125512586,
//            "profile-picture-url",
//            OrgData(
//                85,
//                "org-o",
//            ),
//        )
//        subject.accountData.first().run {
//            assertEquals(523, id)
//            assertEquals("access-token", accessToken)
//            assertEquals("first last", fullName)
//            assertEquals(125512586, tokenExpiry.epochSeconds)
//            assertEquals(OrgData(85, "org-o"), org)
//        }
//
//        subject.clearAccount()
//        subject.accountData.first().run {
//            assertEquals(0, id)
//            assertEquals("", accessToken)
//            assertEquals("", fullName)
//            assertEquals(0, tokenExpiry.epochSeconds)
//            assertEquals(emptyOrgData, org)
//        }
//    }
//
//    @Test
//    fun expireToken() = runTest {
//        subject.setAccount(
//            523,
//            "access-token",
//            "email",
//            "first",
//            "last",
//            125512586,
//            "profile-picture-url",
//            OrgData(
//                85,
//                "org-o",
//            ),
//        )
//
//        subject.expireAccessToken()
//
//        val expected = AccountData(
//            523,
//            "access-token",
//            Instant.fromEpochSeconds(0),
//            "first last",
//            "email",
//            "profile-picture-url",
//            OrgData(
//                85,
//                "org-o",
//            ),
//        )
//        assertEquals(expected, subject.accountData.first())
//    }
}