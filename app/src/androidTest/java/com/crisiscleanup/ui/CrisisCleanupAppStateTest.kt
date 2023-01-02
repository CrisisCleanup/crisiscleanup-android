package com.crisiscleanup.ui

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import com.crisiscleanup.core.testing.util.TestNetworkMonitor
import junit.framework.TestCase.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Tests [CrisisCleanupAppState].
 *
 * Note: This could become an unit test if Robolectric is added to the project and the Context
 * is faked.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class CrisisCleanupAppStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Create the test dependencies.
    private val networkMonitor = TestNetworkMonitor()

    // Subject under test.
    private lateinit var state: CrisisCleanupAppState

    @Test
    fun CrisisCleanupAppState_currentDestination() = runTest {
        var currentDestination: String? = null

        composeTestRule.setContent {
            val navController = rememberTestNavController()
            state = remember(navController) {
                CrisisCleanupAppState(
                    windowSizeClass = getCompactWindowClass(),
                    navController = navController,
                    networkMonitor = networkMonitor,
                    coroutineScope = backgroundScope
                )
            }

            // Update currentDestination whenever it changes
            currentDestination = state.currentDestination?.route

            // Navigate to destination b once
            LaunchedEffect(Unit) {
                navController.setCurrentDestination("b")
            }
        }

        kotlin.test.assertEquals("b", currentDestination)
    }

    @Test
    fun CrisisCleanupAppState_destinations() = runTest {
        composeTestRule.setContent {
            state = rememberCrisisCleanupAppState(
                windowSizeClass = getCompactWindowClass(),
                networkMonitor = networkMonitor
            )
        }

        kotlin.test.assertEquals(3, state.topLevelDestinations.size)
        kotlin.test.assertTrue(state.topLevelDestinations[0].name.contains("for_you", true))
        kotlin.test.assertTrue(state.topLevelDestinations[1].name.contains("bookmarks", true))
        kotlin.test.assertTrue(state.topLevelDestinations[2].name.contains("interests", true))
    }

    @Test
    fun CrisisCleanupAppState_showBottomBar_compact() = runTest {
        composeTestRule.setContent {
            state = CrisisCleanupAppState(
                windowSizeClass = getCompactWindowClass(),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        kotlin.test.assertTrue(state.shouldShowBottomBar)
        kotlin.test.assertFalse(state.shouldShowNavRail)
    }

    @Test
    fun CrisisCleanupAppState_showNavRail_medium() = runTest {
        composeTestRule.setContent {
            state = CrisisCleanupAppState(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 800.dp)),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        kotlin.test.assertTrue(state.shouldShowNavRail)
        kotlin.test.assertFalse(state.shouldShowBottomBar)
    }

    @Test
    fun CrisisCleanupAppState_showNavRail_large() = runTest {

        composeTestRule.setContent {
            state = CrisisCleanupAppState(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(900.dp, 1200.dp)),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        kotlin.test.assertTrue(state.shouldShowNavRail)
        kotlin.test.assertFalse(state.shouldShowBottomBar)
    }

    @Test
    fun stateIsOfflineWhenNetworkMonitorIsOffline() = runTest(UnconfinedTestDispatcher()) {

        composeTestRule.setContent {
            state = CrisisCleanupAppState(
                windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(900.dp, 1200.dp)),
                navController = NavHostController(LocalContext.current),
                networkMonitor = networkMonitor,
                coroutineScope = backgroundScope
            )
        }

        backgroundScope.launch { state.isOffline.collect() }
        networkMonitor.setConnected(false)
        kotlin.test.assertEquals(
            true,
            state.isOffline.value
        )
    }

    private fun getCompactWindowClass() = WindowSizeClass.calculateFromSize(DpSize(500.dp, 300.dp))
}

@Composable
private fun rememberTestNavController(): TestNavHostController {
    val context = LocalContext.current
    val navController = remember {
        TestNavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(startDestination = "a") {
                composable("a") { }
                composable("b") { }
                composable("c") { }
            }
        }
    }
    return navController
}
