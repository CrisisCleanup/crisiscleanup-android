package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.event.CrisisCleanupAuthEventManager
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.datastore.test.testUserPreferencesDataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class OfflineFirstLocalAppPreferencesRepositoryTest {
    private lateinit var subject: OfflineFirstLocalAppPreferencesRepository

    private lateinit var preferencesDataSource: LocalAppPreferencesDataSource

    private lateinit var authEventManager: AuthEventManager

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        preferencesDataSource = LocalAppPreferencesDataSource(
            tmpFolder.testUserPreferencesDataStore()
        )

        authEventManager = CrisisCleanupAuthEventManager()
        subject = OfflineFirstLocalAppPreferencesRepository(
            preferencesDataSource,
            authEventManager,
        )
    }

    @Test
    fun defaultValues() = runTest {
        assertEquals(
            UserData(
                darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                shouldHideOnboarding = false,
                syncAttempt = SyncAttempt(0, 0, 0),
                selectedIncidentId = 0,
                saveCredentialsPromptCount = 0,
                disableSaveCredentialsPrompt = false,
            ),
            subject.userData.first()
        )
    }

    @Test
    fun setDarkThemeConfig_delegatesTo_localAppPreferences() =
        runTest {
            subject.setDarkThemeConfig(DarkThemeConfig.DARK)

            assertEquals(
                DarkThemeConfig.DARK,
                subject.userData
                    .map { it.darkThemeConfig }
                    .first()
            )
            assertEquals(
                DarkThemeConfig.DARK,
                preferencesDataSource
                    .userData
                    .map { it.darkThemeConfig }
                    .first()
            )
        }

    // TODO Other methods delegate to preferences
}
