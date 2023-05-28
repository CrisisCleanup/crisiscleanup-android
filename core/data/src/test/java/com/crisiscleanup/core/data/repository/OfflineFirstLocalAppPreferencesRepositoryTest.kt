package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.event.CrisisCleanupAuthEventBus
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.datastore.test.testUserPreferencesDataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class OfflineFirstLocalAppPreferencesRepositoryTest {
    private lateinit var preferencesDataSource: LocalAppPreferencesDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        preferencesDataSource = LocalAppPreferencesDataSource(
            tmpFolder.testUserPreferencesDataStore()
        )
    }

    private fun setupTestRepository(
        testScheduler: TestCoroutineScheduler,
        testScope: CoroutineScope
    ): Pair<OfflineFirstLocalAppPreferencesRepository, AuthEventBus> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bus = CrisisCleanupAuthEventBus(testScope)
        val repository = OfflineFirstLocalAppPreferencesRepository(
            preferencesDataSource,
            bus,
            testScope,
            dispatcher,
        )
        return Pair(repository, bus)
    }

    @Test
    fun defaultValues() = runTest {
        val (repository, _) = setupTestRepository(testScheduler, this)

        assertEquals(
            UserData(
                darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                shouldHideOnboarding = false,
                syncAttempt = SyncAttempt(0, 0, 0),
                selectedIncidentId = 0,
                saveCredentialsPromptCount = 0,
                disableSaveCredentialsPrompt = false,
                languageKey = "",
            ),
            repository.userData.first()
        )

        repository.observeJobs.forEach(Job::cancel)
    }

    @Test
    fun setDarkThemeConfig_delegatesTo_localAppPreferences() = runTest {
        val (repository, _) = setupTestRepository(testScheduler, this)

        repository.setDarkThemeConfig(DarkThemeConfig.DARK)

        assertEquals(
            DarkThemeConfig.DARK,
            repository.userData
                .map { it.darkThemeConfig }
                .first()
        )
        assertEquals(
            DarkThemeConfig.DARK,
            preferencesDataSource.userData
                .map { it.darkThemeConfig }
                .first()
        )

        repository.observeJobs.forEach(Job::cancel)
    }

    // TODO Other methods delegate to preferences
}
