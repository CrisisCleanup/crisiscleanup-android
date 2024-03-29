package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals

class NetworkLanguageTest {
    @Test
    fun getLanguages() {
        val result = TestUtil.decodeResource<NetworkLanguagesResult>("/getLanguages.json")
        val expected = NetworkLanguagesResult(
            results = listOf(
                NetworkLanguageDescription(2, "en-US", "English (United States)"),
                NetworkLanguageDescription(7, "es-MX", "Spanish (Mexico)"),
                NetworkLanguageDescription(8, "cs", "Czech"),
            ),
        )
        assertEquals(expected, result)
    }

    @Test
    fun getTranslations() {
        val result =
            TestUtil.decodeResource<NetworkLanguageTranslation>("/getLanguageTranslation.json")

        assertEquals("en-US", result.subtag)
        assertEquals("English (United States)", result.name)
        assertEquals(11, result.translations.size)
    }
}
