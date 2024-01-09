package com.crisiscleanup.sandbox

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.PermissionStatus
import com.crisiscleanup.core.common.event.AuthEventBus
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
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@HiltAndroidApp
class SandboxApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var imageLoader: Provider<ImageLoader>

    override fun newImageLoader(): ImageLoader = imageLoader.get()
}

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
    override fun appPull(force: Boolean, cancelOngoing: Boolean) {}

    override suspend fun syncPullAsync() =
        CompletableDeferred(SyncResult.NotAttempted(""))

    override fun stopPull() {}

    override suspend fun syncPullWorksitesFullAsync() =
        CompletableDeferred(SyncResult.NotAttempted(""))

    override fun stopSyncPullWorksitesFull() {}

    override fun scheduleSyncWorksitesFull() {}

    override fun appPullIncident(id: Long) {}

    override suspend fun syncPullIncidentAsync(id: Long) =
        CompletableDeferred(SyncResult.NotAttempted(""))

    override fun stopPullIncident() {}

    override fun appPullIncidentWorksitesDelta() {}

    override fun appPullLanguage() {}

    override suspend fun syncPullLanguage() = SyncResult.NotAttempted("")

    override fun appPullStatuses() {}

    override suspend fun syncPullStatuses() = SyncResult.NotAttempted("")

    override fun appPushWorksite(worksiteId: Long) {}

    override suspend fun syncPushWorksitesAsync() = CompletableDeferred(SyncResult.NotAttempted(""))

    override fun stopPushWorksites() {}

    override suspend fun syncPushMedia() = SyncResult.NotAttempted("")

    override suspend fun syncPushWorksites() = SyncResult.NotAttempted("")

    override fun scheduleSyncMedia() {}

    override fun scheduleSyncWorksites() {}
}

@Singleton
class AppAuthEventBus @Inject constructor() : AuthEventBus {
    override val logouts = MutableStateFlow(false)
    override val refreshedTokens = MutableStateFlow(false)

    override fun onLogout() {}

    override fun onTokensRefreshed() {}
}

@Singleton
class AppLocationProvider @Inject constructor() : LocationProvider {
    override val coordinates: Pair<Double, Double> = Pair(0.0, 0.0)

    override suspend fun getLocation() = coordinates
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
    fun bindsAuthEventBus(eventBus: AppAuthEventBus): AuthEventBus

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
}
