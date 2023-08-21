package com.crisiscleanup.core.common

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface AppMemoryStats {
    val isLowMemory: Boolean

    val availableMemory: Int
}

class AndroidAppMemoryStats @Inject constructor(
    @ApplicationContext context: Context,
) : AppMemoryStats {
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val memoryInfo: ActivityManager.MemoryInfo
        get() = ActivityManager.MemoryInfo().also {
            activityManager.memoryClass
        }

    override val isLowMemory: Boolean
        get() = memoryInfo.lowMemory

    override val availableMemory: Int
        get() = activityManager.memoryClass
}
