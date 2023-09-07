package com.crisiscleanup.core.common

interface AppEnv {
    val isDebuggable: Boolean
    val isProduction: Boolean
    val isNotProduction: Boolean
    val isEarlybird: Boolean

    val apiEnvironment: String
    fun runInNonProd(block: () -> Unit)
}
