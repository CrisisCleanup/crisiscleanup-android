package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import com.crisiscleanup.core.common.epochZero
import com.crisiscleanup.core.model.data.Language
import com.crisiscleanup.core.model.data.LanguageTranslations
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

data class PopulatedLanguage(
    val key: String,
    val name: String,
)

data class PopulatedLanguageTranslation(
    @Embedded
    val entity: LanguageTranslationEntity,
)

fun PopulatedLanguageTranslation.asExternalModel(): LanguageTranslations {
    var translations = emptyMap<String, String>()
    entity.translationsJson?.let {
        if (it.isNotEmpty()) {
            translations = Json.decodeFromString(entity.translationsJson)
        }
    }
    return LanguageTranslations(
        language = Language(key = entity.key, displayName = entity.name),
        translations = translations,
        syncedAt = entity.syncedAt ?: Instant.epochZero,
    )
}
