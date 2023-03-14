package com.crisiscleanup.navigation

import android.os.Bundle
import com.crisiscleanup.core.appheader.AppHeaderState
import com.crisiscleanup.core.appheader.AppHeaderUiState
import com.crisiscleanup.core.appnav.RouteConstant.caseEditorRoute
import com.crisiscleanup.core.appnav.RouteConstant.casesRoute
import com.crisiscleanup.core.appnav.RouteConstant.topLevelRoutes
import com.crisiscleanup.core.common.NavigationObserver
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    init {
        navigationRoute.onEach {
            val (fromRoute, toRoute) = navigationRoute.value

            // TODO Research a better pattern when there is a shared top level component between routes
            if (fromRoute == casesRoute &&
                toRoute != casesRoute &&
                headerUiState.appHeaderState.value == AppHeaderState.SearchInTitle
            ) {
                headerUiState.setState(AppHeaderState.TopLevel)
            } else if (toRoute?.startsWith("$caseEditorRoute?") == true) {
                headerUiState.setState(AppHeaderState.BackTitleAction)
            }

            if (topLevelRoutes.contains(toRoute) &&
                !topLevelRoutes.contains(fromRoute)
            ) {
                headerUiState.setState(AppHeaderState.TopLevel)
            }
        }
            .launchIn(coroutineScope)
    }
}