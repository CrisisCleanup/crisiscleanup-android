package com.crisiscleanup.core.common

import android.os.Bundle

interface NavigationObserver {
    fun onRouteChange(route: String?, arguments: Bundle?)
}
