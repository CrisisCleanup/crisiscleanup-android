package com.crisiscleanup.core.datastore

import com.crisiscleanup.core.datastore.test.testUserPreferencesDataStore
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAppPreferencesDataSourceTest {
    private lateinit var subject: LocalAppPreferencesDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        subject = LocalAppPreferencesDataSource(
            tmpFolder.testUserPreferencesDataStore(),
        )
    }

    @Test
    fun shouldHideOnboardingIsFalseByDefault() = runTest {
        assertFalse(subject.userData.first().shouldHideOnboarding)
    }

    @Test
    fun userShouldHideOnboardingIsTrueWhenSet() = runTest {
        subject.setShouldHideOnboarding(true)
        assertTrue(subject.userData.first().shouldHideOnboarding)
    }

    @Test
    fun syncAttemptDefault() = runTest {
        val syncAttempt = SyncAttempt(0, 0, 0)
        assertEquals(syncAttempt, subject.userData.first().syncAttempt)
    }

    private fun setupSyncAttempt(
        testBody: suspend TestScope.() -> Unit,
        onAttempts: TestScope.(List<SyncAttempt>) -> Unit,
    ) = runTest {
        val values = mutableListOf<UserData>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            subject.userData.toList(values)
        }

        try {
            testBody()

            advanceUntilIdle()
            yield()

            val attempts = values.map(UserData::syncAttempt)
            onAttempts(attempts)
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun syncAttemptSuccessful() = runTest {
        setupSyncAttempt(
            {
                subject.setSyncAttempt(true, 1582)
                subject.setSyncAttempt(true, 19815)
                subject.setSyncAttempt(false, 20158)
            },
        ) { attempts: List<SyncAttempt> ->
            val expecteds = listOf(
                SyncAttempt(0, 0, 0),
                SyncAttempt(1582, 1582, 0),
                SyncAttempt(19815, 19815, 0),
                SyncAttempt(19815, 20158, 1),
            )
            for (i in expecteds.indices) {
                assertEquals(expecteds[i], attempts[i])
            }
        }
    }

    @Test
    fun syncAttemptFail() = runTest {
        setupSyncAttempt(
            {
                subject.setSyncAttempt(false, 1582)
                subject.setSyncAttempt(false, 19815)
                subject.setSyncAttempt(true, 20158)
            },
        ) { attempts: List<SyncAttempt> ->
            val expecteds = listOf(
                SyncAttempt(0, 0, 0),
                SyncAttempt(0, 1582, 1),
                SyncAttempt(0, 19815, 2),
                SyncAttempt(20158, 20158, 0),
            )
            for (i in expecteds.indices) {
                // Without print statements this will fail at times due to no attempts...
                val attempt = attempts[i]
                val expected = expecteds[i]
                assertEquals(expected, attempt)
            }
        }
    }

    @Test
    fun clearSyncData() = runTest {
        subject.setSyncAttempt(true, 20158)
        subject.setSyncAttempt(false, 58354)

        val syncAttempt = subject.userData.first().syncAttempt
        assertEquals(SyncAttempt(20158, 58354, 1), syncAttempt)

        subject.clearSyncData()
        val clearedSyncAttempt = subject.userData.first().syncAttempt
        assertEquals(SyncAttempt(0, 0, 0), clearedSyncAttempt)
    }
}
