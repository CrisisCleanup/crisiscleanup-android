package com.crisiscleanup.core.network.retrofit

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION)
@Retention(RUNTIME)
internal annotation class WrapIncidentResponseHeader