package com.crisiscleanup.core.network.retrofit

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

@Target(FUNCTION)
@Retention(RUNTIME)
internal annotation class WrapResponseHeader(val key: String)
