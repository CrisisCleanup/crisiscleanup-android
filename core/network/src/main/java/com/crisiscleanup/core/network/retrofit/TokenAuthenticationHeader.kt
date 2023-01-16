package com.crisiscleanup.core.network.retrofit

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY_GETTER)
@Retention(RUNTIME)
internal annotation class TokenAuthenticationHeader