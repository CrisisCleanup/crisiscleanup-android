package com.crisiscleanup.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val crisisCleanupDispatcher: CrisisCleanupDispatchers)

enum class CrisisCleanupDispatchers {
    IO
}
