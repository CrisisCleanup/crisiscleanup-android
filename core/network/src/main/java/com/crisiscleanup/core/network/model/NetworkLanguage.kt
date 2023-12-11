package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkLanguagesResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val results: List<NetworkLanguageDescription>,
)

@Serializable
data class NetworkLanguageDescription(
    val id: Long,
    val subtag: String,
    @SerialName("name_t")
    val name: String,
)

@Serializable
data class NetworkLanguageTranslationResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val translation: NetworkLanguageTranslation? = null,
)

@Serializable
data class NetworkLanguageTranslation(
    val subtag: String,
    @SerialName("name_t")
    val name: String,
    val translations: Map<String, String?>,
)
