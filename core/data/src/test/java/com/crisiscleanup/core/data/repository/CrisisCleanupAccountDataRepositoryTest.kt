package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.datastore.test.testAccountInfoDataStore
import com.crisiscleanup.core.model.data.AccountData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CrisisCleanupAccountDataRepositoryTest {
    private lateinit var subject: CrisisCleanupAccountDataRepository

    private lateinit var accountInfoDataSource: AccountInfoDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        accountInfoDataSource = AccountInfoDataSource(
            tmpFolder.testAccountInfoDataStore()
        )

        subject = CrisisCleanupAccountDataRepository(accountInfoDataSource)
    }

    @Test
    fun defaultIsUnauthenticated() = runTest {
        subject.accountData.first().let {
            assertEquals(
                AccountData(
                    accessToken = "",
                    displayName = "",
                    tokenExpiry = Instant.fromEpochSeconds(0),
                ),
                it
            )
        }
        assertFalse(subject.isAuthenticated.first())
    }

    @Test
    fun setAccount_clearAccount_delegatesTo_dataSource() = runTest {
        subject.setAccount("at", "em", "fn", "ln", 6235234341)
        var expectedData = AccountData(
            accessToken = "at",
            displayName = "fn ln",
            tokenExpiry = Instant.fromEpochSeconds(6235234341)
        )
        assertEquals(expectedData, subject.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())

        subject.clearAccount()
        expectedData = AccountData(
            accessToken = "",
            displayName = "",
            tokenExpiry = Instant.fromEpochSeconds(0)
        )
        assertEquals(expectedData, subject.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())
    }
}