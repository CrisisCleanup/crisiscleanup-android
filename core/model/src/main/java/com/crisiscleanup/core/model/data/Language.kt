package com.crisiscleanup.core.model.data

import kotlin.time.Instant

data class Language(
    val key: String,
    val displayName: String,
)

data class LanguageTranslations(
    val language: Language,
    val translations: Map<String, String>,
    val syncedAt: Instant,
)

val EnglishLanguage = Language("en-US", "English (United States)")
