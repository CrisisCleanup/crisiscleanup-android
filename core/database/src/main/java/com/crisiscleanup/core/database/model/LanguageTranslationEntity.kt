package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity("language_translations")
data class LanguageTranslationEntity(
    @PrimaryKey
    val key: String,
    val name: String,
    @ColumnInfo("translations_json")
    val translationsJson: String?,
    @ColumnInfo("synced_at", defaultValue = "0")
    val syncedAt: Instant?,
)