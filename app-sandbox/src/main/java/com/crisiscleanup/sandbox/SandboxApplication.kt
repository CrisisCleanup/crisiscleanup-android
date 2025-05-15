package com.crisiscleanup.sandbox

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.util.Log
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.log.TagLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.network.AuthInterceptorProvider
import com.crisiscleanup.core.network.RetrofitInterceptorProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

@HiltAndroidApp
class SandboxApplication : Application()

@Singleton
class SandboxAppEnv @Inject constructor() : AppEnv {
    override val isDebuggable = true
    override val isProduction = false
    override val isNotProduction = true

    override val isEarlybird = false

    override val apiEnvironment: String
        get() = "Sandbox"

    override fun runInNonProd(block: () -> Unit) {
        block()
    }
}

class AppLogger @Inject constructor() : TagLogger {
    override var tag: String? = null

    override fun logDebug(vararg logs: Any) {
        Log.d(tag, logs.joinToString(" "))
    }

    override fun logException(e: Exception) {
        if (e is CancellationException) {
            return
        }

        Log.e(tag, e.message, e)
    }

    override fun logCapture(message: String) {
        Log.w(tag, message)
    }

    override fun setAccountId(id: String) {}
}

@Singleton
class AppInterceptorProvider @Inject constructor() : RetrofitInterceptorProvider {
    override val serverErrorInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            chain.proceed(request)
        }
    }

    override val interceptors: List<Interceptor> = listOf(
        serverErrorInterceptor,
    )
}

@Singleton
class AppAuthInterceptProvider @Inject constructor() : AuthInterceptorProvider {
    override val clientErrorInterceptor by lazy {
        Interceptor { chain ->
            val request = chain.request()
            chain.proceed(request)
        }
    }
}

@Singleton
class AppSyncer @Inject constructor() : SyncPuller, SyncPusher {
    override fun appPullIncidentData(
        cancelOngoing: Boolean,
        forcePullIncidents: Boolean,
        cacheSelectedIncident: Boolean,
        cacheActiveIncidentWorksites: Boolean,
        cacheFullWorksites: Boolean,
        restartCacheCheckpoint: Boolean,
    ) {
    }

    override suspend fun syncPullIncidentData(
        cancelOngoing: Boolean,
        forcePullIncidents: Boolean,
        cacheSelectedIncident: Boolean,
        cacheActiveIncidentWorksites: Boolean,
        cacheFullWorksites: Boolean,
        restartCacheCheckpoint: Boolean,
    ) = SyncResult.NotAttempted("")

    override fun stopPullWorksites() {}

    override fun appPullLanguage() {}

    override suspend fun syncPullLanguage() = SyncResult.NotAttempted("")

    override fun appPullStatuses() {}

    override suspend fun syncPullStatuses() = SyncResult.NotAttempted("")

    override fun appPushWorksite(worksiteId: Long, scheduleMediaSync: Boolean) {}

    override suspend fun syncPushWorksitesAsync() = CompletableDeferred(SyncResult.NotAttempted(""))

    override suspend fun syncPushMedia() = SyncResult.NotAttempted("")

    override suspend fun syncPushWorksites() = SyncResult.NotAttempted("")

    override fun scheduleSyncMedia() {}

    override fun scheduleSyncWorksites() {}
}

@Singleton
class AppAccountEventBus @Inject constructor() : AccountEventBus {
    override val logouts = MutableStateFlow(false)
    override val refreshedTokens = MutableStateFlow(false)
    override val inactiveOrganizations = MutableStateFlow(0L)

    override fun onLogout() {}

    override fun onTokensRefreshed() {}

    override fun onAccountInactiveOrganization(accountId: Long) {}
    override fun clearAccountInactiveOrganization() {}
}

@Singleton
class AppLocationProvider @Inject constructor() : LocationProvider {
    override val coordinates = Pair(0.0, 0.0)

    override suspend fun getLocation(timeout: Duration) = coordinates
}

@Singleton
class AppPermissionManager @Inject constructor() : PermissionManager {
    override val hasLocationPermission = MutableStateFlow(true)
    override val permissionChanges = MutableStateFlow(Pair("", PermissionStatus.Requesting))

    override fun requestLocationPermission() = PermissionStatus.Granted

    override fun requestCameraPermission() = PermissionStatus.Granted

    override fun requestScreenshotReadPermission() = PermissionStatus.Granted
}

@Singleton
class AppWorksiteProvider @Inject constructor() : WorksiteProvider {
    override val editableWorksite = MutableStateFlow(EmptyWorksite)
    override var workTypeTranslationLookup: Map<String, String> = mutableMapOf()

    override fun translate(key: String): String = key
}

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Binds
    fun bindsAppEnv(appEnv: SandboxAppEnv): AppEnv

    @Binds
    fun bindsTagLogger(logger: AppLogger): TagLogger

    @Binds
    fun bindsInterceptorProvider(provider: AppInterceptorProvider): RetrofitInterceptorProvider

    @Binds
    fun bindsAuthInterceptorProvider(provider: AppAuthInterceptProvider): AuthInterceptorProvider

    @Binds
    fun bindsSyncPuller(syncer: AppSyncer): SyncPuller

    @Binds
    fun bindsSyncPusher(syncer: AppSyncer): SyncPusher

    @Binds
    fun bindsAccountEventBus(eventBus: AppAccountEventBus): AccountEventBus

    @Binds
    fun bindsLocationProvider(provider: AppLocationProvider): LocationProvider

    @Binds
    fun bindsPermissionManager(manager: AppPermissionManager): PermissionManager

    @Binds
    fun bindsWorksiteProvider(provider: AppWorksiteProvider): WorksiteProvider
}

@Module
@InstallIn(SingletonComponent::class)
object AppObjectModule {
    @Provides
    fun providesPackageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager

    @Provides
    fun providesContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    fun providesConnectivityManager(
        @ApplicationContext context: Context,
    ): ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
}
