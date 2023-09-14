package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.LanguageTranslationEntity
import com.crisiscleanup.core.network.model.NetworkLanguageDescription
import com.crisiscleanup.core.network.model.NetworkLanguageTranslation
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun NetworkLanguageDescription.asEntity() = LanguageTranslationEntity(
    key = subtag,
    name = name,
    translationsJson = null,
    syncedAt = null,
)

fun NetworkLanguageTranslation.asEntity(syncedAt: Instant) = LanguageTranslationEntity(
    key = subtag,
    name = name,
    translationsJson = Json.encodeToString(translations.filter { it.value != null }),
    syncedAt = syncedAt,
)
