package com.crisiscleanup

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.crisiscleanup.sync.initializers.Sync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class CrisisCleanupApplication : Application(), ImageLoaderFactory {
    @Inject
    lateinit var imageLoader: Provider<ImageLoader>

    override fun onCreate() {
        super.onCreate()

        Sync.initialize(this)
    }

    override fun newImageLoader(): ImageLoader = imageLoader.get()
}
