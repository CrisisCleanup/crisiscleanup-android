package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.StateFlow

interface KeyTranslator {
    val translationCount: StateFlow<Int>
    fun translate(phraseKey: String): String?
}
