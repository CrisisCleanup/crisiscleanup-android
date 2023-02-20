package com.crisiscleanup.feature.menu

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.AppVersionProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val appVersionProvider: AppVersionProvider
) : ViewModel() {
    val versionText: String
        get() {
            val version = appVersionProvider.version
            return "${version.second} (${version.first})"
        }
}