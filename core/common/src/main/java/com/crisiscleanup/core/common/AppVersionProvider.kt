package com.crisiscleanup.core.common

import android.content.Context
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AppVersionProvider {
    val versionCode: Int
}

@Singleton
class AndroidAppVersionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppVersionProvider {
    override val versionCode: Int
        get() {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            return packageInfo.longVersionCode.toInt()
        }
}