package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.CrisisCleanupAuthEventManager
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.datastore.test.testAccountInfoDataStore
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyOrgData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrisisCleanupAccountDataRepositoryTest {
    private lateinit var subject: CrisisCleanupAccountDataRepository

    private lateinit var accountInfoDataSource: AccountInfoDataSource

    private lateinit var authEventManager: AuthEventManager

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() = runTest {
        accountInfoDataSource = AccountInfoDataSource(
            tmpFolder.testAccountInfoDataStore()
        )

        authEventManager = CrisisCleanupAuthEventManager()

        val dispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(dispatcher)
        subject = CrisisCleanupAccountDataRepository(
            accountInfoDataSource,
            authEventManager,
            testScope,
            dispatcher,
        )
    }

    @Test
    fun defaultIsUnauthenticated() = runTest {
        assertTrue(subject.accessTokenCached.isEmpty())

        subject.accountData.first().let {
            assertEquals(
                AccountData(
                    id = 0,
                    accessToken = "",
                    fullName = "",
                    tokenExpiry = Instant.fromEpochSeconds(0),
                    emailAddress = "",
                    profilePictureUri = "",
                    org = emptyOrgData,
                ),
                it
            )
        }

        assertFalse(subject.isAuthenticated.first())
    }

    @Test
    fun setAccount_logout_delegatesTo_dataSource() = runTest {
        subject.setAccount(
            5434,
            "at",
            "em",
            "fn",
            "ln",
            6235234341,
            "pp",
            org = OrgData(83, "org"),
        )
        var expectedData = AccountData(
            id = 5434,
            accessToken = "at",
            fullName = "fn ln",
            tokenExpiry = Instant.fromEpochSeconds(6235234341),
            emailAddress = "em",
            profilePictureUri = "pp",
            org = OrgData(83, "org"),
        )

        assertEquals("at", subject.accessTokenCached)
        assertEquals(expectedData, subject.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())
        assertTrue(subject.isAuthenticated.first())

        authEventManager.onLogout()

        expectedData = AccountData(
            id = 0,
            accessToken = "",
            fullName = "",
            tokenExpiry = Instant.fromEpochSeconds(0),
            emailAddress = "",
            profilePictureUri = "",
            org = emptyOrgData,
        )

        assertTrue(subject.accessTokenCached.isEmpty())
        assertEquals(expectedData, subject.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())
        assertFalse(subject.isAuthenticated.first())
    }

    @Test
    fun onExpiredToken() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val manager = CrisisCleanupAuthEventManager()
        val repository = CrisisCleanupAccountDataRepository(
            accountInfoDataSource,
            manager,
            this,
            dispatcher,
        )

        repository.setAccount(
            5434,
            "at",
            "em",
            "fn",
            "ln",
            6235234341,
            "pp",
            org = OrgData(83, "org"),
        )

        manager.onExpiredToken()

        delay(99)
        val delayed = advanceUntilIdle()
        println("Logging delay $delayed so the test passes...")

        val expectedData = AccountData(
            5434,
            "",
            Instant.fromEpochSeconds(0),
            "fn ln",
            "em",
            "pp",
            OrgData(83, "org"),
        )

        assertTrue(repository.accessTokenCached.isEmpty())
        assertEquals(expectedData, repository.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())
        assertFalse(repository.isAuthenticated.first())
    }
}