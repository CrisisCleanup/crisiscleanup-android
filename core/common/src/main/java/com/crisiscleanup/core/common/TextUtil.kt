package com.crisiscleanup.core.common

fun Collection<String?>.filterNotBlankTrim(): List<String> {
    val notBlank = filter { it?.isNotBlank() == true }.filterNotNull()
    return notBlank.map(String::trim)
}

fun Collection<String>.combineTrimText() = filterNotBlankTrim().joinToString(", ")
