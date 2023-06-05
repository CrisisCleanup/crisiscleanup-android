package com.crisiscleanup.core.common

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

interface KeyTranslator {
    val translationCount: StateFlow<Int>
    fun translate(phraseKey: String): String?
}

interface KeyResourceTranslator : KeyTranslator {
    fun translate(phraseKey: String, @StringRes fallbackResId: Int): String

    operator fun invoke(phraseKey: String, @StringRes fallbackResId: Int) =
        translate(phraseKey, fallbackResId)
}

class AndroidResourceTranslator @Inject constructor(
    private val keyTranslator: KeyTranslator,
    @ApplicationContext private val context: Context,
) : KeyResourceTranslator {
    override val translationCount = keyTranslator.translationCount

    override fun translate(phraseKey: String) = keyTranslator.translate(phraseKey)

    override fun translate(phraseKey: String, @StringRes fallbackResId: Int) =
        keyTranslator.translate(phraseKey) ?: context.getString(fallbackResId).also {
            Log.w("resource-translate", "Translate from resource $phraseKey $fallbackResId")
        }
}