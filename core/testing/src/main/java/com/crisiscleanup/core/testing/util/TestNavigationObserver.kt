package com.crisiscleanup.core.testing.util

import android.os.Bundle
import com.crisiscleanup.core.common.NavigationObserver

class TestNavigationObserver : NavigationObserver {
    override fun onRouteChange(route: String?, arguments: Bundle?) {}
}
