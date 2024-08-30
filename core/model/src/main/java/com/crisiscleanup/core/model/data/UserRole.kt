package com.crisiscleanup.core.model.data

data class UserRole(
    val id: Int,
    val nameKey: String,
    val name: String = nameKey,
    val descriptionKey: String,
    val description: String = descriptionKey,
    val level: Int,
)
