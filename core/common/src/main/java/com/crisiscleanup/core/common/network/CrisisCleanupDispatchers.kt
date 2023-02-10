package com.crisiscleanup.core.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val dispatchers: CrisisCleanupDispatchers)

enum class CrisisCleanupDispatchers {
    Default,
    IO,
}
