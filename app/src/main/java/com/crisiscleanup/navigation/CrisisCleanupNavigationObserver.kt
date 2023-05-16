package com.crisiscleanup.navigation

import android.os.Bundle
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisCleanupNavigationObserver @Inject constructor(
    private val headerUiState: AppHeaderUiState,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    @Logger(CrisisCleanupLoggers.Navigation) logger: AppLogger,
) : NavigationObserver {
    private val navigationRoute = MutableStateFlow<Pair<String?, String?>>(Pair(null, null))

    override fun onRouteChange(route: String?, arguments: Bundle?) {
        navigationRoute.value = Pair(navigationRoute.value.second, route)
    }
}