package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.event.CrisisCleanupAuthEventBus
import com.crisiscleanup.core.datastore.AccountInfoDataSource
import com.crisiscleanup.core.datastore.test.testAccountInfoDataStore
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.OrgData
import com.crisiscleanup.core.model.data.emptyOrgData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
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
    private lateinit var accountInfoDataSource: AccountInfoDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        accountInfoDataSource = AccountInfoDataSource(
            tmpFolder.testAccountInfoDataStore()
        )
    }

    private fun setupTestRepository(
        testScheduler: TestCoroutineScheduler,
        testScope: CoroutineScope
    ): Pair<CrisisCleanupAccountDataRepository, AuthEventBus> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bus = CrisisCleanupAuthEventBus(testScope)
        val repository = CrisisCleanupAccountDataRepository(
            accountInfoDataSource,
            bus,
            testScope,
            dispatcher,
        )
        return Pair(repository, bus)
    }

    @Test
    fun defaultIsUnauthenticated() = runTest {
        val (repository, _) = setupTestRepository(testScheduler, this)

        assertTrue(repository.accessTokenCached.isEmpty())

        repository.accountData.first().let {
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

        assertFalse(repository.isAuthenticated.first())

        repository.observeJobs.forEach(Job::cancel)
    }

    @Test
    fun setAccount_logout_delegatesTo_dataSource() = runTest {
        val (repository, bus) = setupTestRepository(testScheduler, this)

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

        var expectedData = AccountData(
            id = 5434,
            accessToken = "at",
            fullName = "fn ln",
            tokenExpiry = Instant.fromEpochSeconds(6235234341),
            emailAddress = "em",
            profilePictureUri = "pp",
            org = OrgData(83, "org"),
        )

        assertEquals("at", repository.accessTokenCached)
        assertEquals(expectedData, repository.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())
        assertTrue(repository.isAuthenticated.first())

        // TODO How to wait for suspending functions to complete then cancel jobs and continue with test?
        bus.onLogout()

        expectedData = AccountData(
            id = 0,
            accessToken = "",
            fullName = "",
            tokenExpiry = Instant.fromEpochSeconds(0),
            emailAddress = "",
            profilePictureUri = "",
            org = emptyOrgData,
        )

        assertTrue(repository.accessTokenCached.isEmpty())
        assertEquals(expectedData, repository.accountData.first())
        assertEquals(expectedData, accountInfoDataSource.accountData.first())
        assertFalse(repository.isAuthenticated.first())
    }

    @Test
    fun onExpiredToken() = runTest {
        val (repository, bus) = setupTestRepository(testScheduler, this)

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

        // TODO How to wait for suspending functions to complete then cancel jobs and continue with test?
        bus.onExpiredToken()

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