/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.crisiscleanup.core.data.repository.fake

import com.crisiscleanup.core.data.repository.UserDataRepository
import com.crisiscleanup.core.datastore.CrisisCleanupPreferencesDataSource
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Fake implementation of the [UserDataRepository] that returns hardcoded user data.
 *
 * This allows us to run the app with fake data, without needing an internet connection or working
 * backend.
 */
class FakeUserDataRepository @Inject constructor(
    private val preferencesDataSource: CrisisCleanupPreferencesDataSource,
) : UserDataRepository {

    override val userData: Flow<UserData> =
        preferencesDataSource.userData

    override suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        preferencesDataSource.setDarkThemeConfig(darkThemeConfig)
    }

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        preferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)
    }
}
