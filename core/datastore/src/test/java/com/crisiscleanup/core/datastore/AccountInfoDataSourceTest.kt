package com.crisiscleanup.core.datastore

import com.crisiscleanup.core.datastore.test.testAccountInfoDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountInfoDataSourceTest {
    private lateinit var subject: AccountInfoDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        subject = AccountInfoDataSource(
            tmpFolder.testAccountInfoDataStore()
        )
    }

    @Test
    fun unauthenticatedAccountByDefault() = runTest {
        assertTrue { subject.accountData.first().accessToken.isEmpty() }
    }

    @Test
    fun setAccount_clearAccount() = runTest {
        subject.setAccount(
            "access-token",
            "email",
            "first",
            "last",
            125512586,
            "profile-picture-url",
        )
        subject.accountData.first().run {
            assertEquals("access-token", accessToken)
            assertEquals("first last", displayName)
            assertEquals(125512586, tokenExpiry.epochSeconds)
        }

        subject.clearAccount()
        subject.accountData.first().run {
            assertEquals("", accessToken)
            assertEquals("", displayName)
            assertEquals(0, tokenExpiry.epochSeconds)
        }
    }
}