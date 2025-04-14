package com.crisiscleanup.core.network.retrofit

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
internal annotation class ConnectTimeoutHeader(val timeoutSeconds: String)

@Target(FUNCTION)
@Retention(RUNTIME)
internal annotation class ReadTimeoutHeader(val timeoutSeconds: String)
