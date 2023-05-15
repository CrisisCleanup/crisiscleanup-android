package com.crisiscleanup.core.common

interface AppEnv {
    val isDebuggable: Boolean
    val isProduction: Boolean
    val isNotProduction: Boolean
    fun runInNonProd(block: () -> Unit)
}