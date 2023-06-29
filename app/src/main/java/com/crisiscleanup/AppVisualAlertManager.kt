package com.crisiscleanup

import com.crisiscleanup.core.common.VisualAlertManager
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppVisualAlertManager @Inject constructor() : VisualAlertManager {
    private var showNonProductionAlert = AtomicBoolean(true)

    override fun takeNonProductionAppAlert(): Boolean {
        return showNonProductionAlert.getAndSet(false)
    }

    override fun setNonProductionAppAlert(show: Boolean) {
        showNonProductionAlert.set(show)
    }
}