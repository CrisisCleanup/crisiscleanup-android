package com.crisiscleanup.sync

import android.content.Context
import com.crisiscleanup.core.common.Syncer
import com.crisiscleanup.sync.initializers.scheduleSync
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForcibleSyncer @Inject constructor(
    @ApplicationContext private val context: Context,
) : Syncer {
    override suspend fun sync(force: Boolean) = scheduleSync(context, force)
}