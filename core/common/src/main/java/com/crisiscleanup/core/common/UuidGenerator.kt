package com.crisiscleanup.core.common

import java.util.UUID
import javax.inject.Inject

interface UuidGenerator {
    fun uuid(): String
}

class JavaUuidGenerator @Inject constructor() : UuidGenerator {
    override fun uuid() = UUID.randomUUID().toString()
}