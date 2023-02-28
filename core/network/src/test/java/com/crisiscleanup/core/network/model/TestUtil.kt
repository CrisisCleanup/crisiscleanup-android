package com.crisiscleanup.core.network.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object TestUtil {
    val json = Json { ignoreUnknownKeys = true }

    fun loadFile(filePath: String) =
        TestUtil::class.java.getResource(filePath)?.readText()!!

    inline fun <reified T> decodeResource(filePath: String) =
        json.decodeFromString<T>(loadFile(filePath))
}