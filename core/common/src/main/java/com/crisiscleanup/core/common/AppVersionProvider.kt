package com.crisiscleanup.core.common

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AppVersionProvider {
    /**
     * Code is first, name is second
     */
    val version: Pair<Long, String>
    val versionCode: Long
    val versionName: String
}

interface DatabaseVersionProvider {
    val databaseVersion: Int
}

@Singleton
class AndroidAppVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppVersionProvider {
    private val packageInfo: PackageInfo
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageInfoFlags.of(0),
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

    override val version: Pair<Long, String> by lazy {
        val code = packageInfo.longVersionCode
        val name = packageInfo.versionName ?: ""
        Pair(code, name)
    }

    override val versionCode: Long
        get() = version.first

    override val versionName: String
        get() = version.second
}
