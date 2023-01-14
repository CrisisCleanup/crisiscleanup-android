package com.crisiscleanup

import android.app.Application
import android.content.pm.ApplicationInfo
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp

/**
 * [Application] class for Crisis Cleanup
 */
@HiltAndroidApp
class CrisisCleanupApplication : Application(), ImageLoaderFactory {
    companion object {
        var isDebuggable: Boolean = false
            private set
    }

    override fun onCreate() {
        isDebuggable = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        super.onCreate()

        // TODO Add syncing or delete
        // Initialize Sync; the system responsible for keeping data in the app up to date.
        // Sync.initialize(context = this)
    }

    /**
     * Since we're displaying SVGs in the app, Coil needs an ImageLoader which supports this
     * format. During Coil's initialization it will call `applicationContext.newImageLoader()` to
     * obtain an ImageLoader.
     *
     * @see https://github.com/coil-kt/coil/blob/main/coil-singleton/src/main/java/coil/Coil.kt#L63
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
}
