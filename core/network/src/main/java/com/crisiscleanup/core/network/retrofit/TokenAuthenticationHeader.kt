package com.crisiscleanup.core.network.retrofit

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER

@Target(FUNCTION, PROPERTY_GETTER)
@Retention(RUNTIME)
internal annotation class TokenAuthenticationHeader
