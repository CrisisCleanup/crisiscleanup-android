package com.crisiscleanup.core.common

interface VisualAlertManager {
    fun takeNonProductionAppAlert(): Boolean
    fun setNonProductionAppAlert(show: Boolean)
}