package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.CrisisCleanupPreferencesDataSource
import com.crisiscleanup.core.datastore.test.testUserPreferencesDataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

class OfflineFirstUserDataRepositoryTest {
    private lateinit var subject: OfflineFirstUserDataRepository

    private lateinit var preferencesDataSource: CrisisCleanupPreferencesDataSource

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    @Before
    fun setup() {
        preferencesDataSource = CrisisCleanupPreferencesDataSource(
            tmpFolder.testUserPreferencesDataStore()
        )

        subject = OfflineFirstUserDataRepository(
            preferencesDataSource = preferencesDataSource
        )
    }

    @Test
    fun offlineFirstUserDataRepository_default_user_data_is_correct() =
        runTest {
            assertEquals(
                UserData(
                    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                    shouldHideOnboarding = false
                ),
                subject.userData.first()
            )
        }

    @Test
    fun offlineFirstUserDataRepository_set_dark_theme_config_delegates_to_CrisisCleanup_preferences() =
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
}
