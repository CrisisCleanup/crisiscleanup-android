package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.event.CrisisCleanupAccountEventBus
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.datastore.test.testUserPreferencesDataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.testing.model.UserDataNone
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

class AppPreferencesRepositoryTest {
    private lateinit var preferencesDataSource: LocalAppPreferencesDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        preferencesDataSource = LocalAppPreferencesDataSource(
            tmpFolder.testUserPreferencesDataStore(),
        )
    }

    private fun setupTestRepository(
        testScheduler: TestCoroutineScheduler,
        testScope: CoroutineScope,
    ): Pair<AppPreferencesRepository, AccountEventBus> {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bus = CrisisCleanupAccountEventBus(testScope)
        val repository = AppPreferencesRepository(
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
            UserDataNone,
            repository.userPreferences.first(),
        )

        repository.observeJobs.forEach(Job::cancel)
    }

    @Test
    fun setDarkThemeConfig_delegatesTo_localAppPreferences() = runTest {
        val (repository, _) = setupTestRepository(testScheduler, this)

        repository.setDarkThemeConfig(DarkThemeConfig.DARK)

        assertEquals(
            DarkThemeConfig.DARK,
            repository.userPreferences
                .map { it.darkThemeConfig }
                .first(),
        )
        assertEquals(
            DarkThemeConfig.DARK,
            preferencesDataSource.userData
                .map { it.darkThemeConfig }
                .first(),
        )

        repository.observeJobs.forEach(Job::cancel)
    }

    // TODO Other methods delegate to preferences
}
