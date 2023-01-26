package com.crisiscleanup.sync

import android.content.Context
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.sync.initializers.scheduleSync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackoffSyncer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: LocalAppPreferencesDataSource,
) : Syncer {
    override suspend fun sync(force: Boolean) {
        if (!force) {
            val syncAttempt = appPreferences.userData.first().syncAttempt
            if (syncAttempt.isRecent() || syncAttempt.isBackingOff()) {
                return
            }
        }

        scheduleSync(context)
    }
}