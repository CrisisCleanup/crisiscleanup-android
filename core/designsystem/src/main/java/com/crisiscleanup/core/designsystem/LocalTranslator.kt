package com.crisiscleanup.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import com.crisiscleanup.core.common.KeyResourceTranslator
import kotlinx.coroutines.flow.MutableStateFlow

data class AppTranslator(
    val translator: KeyResourceTranslator
)

//private val passThroughTranslator = AppTranslator(
//    translator = object : KeyResourceTranslator {
//        override val translationCount = MutableStateFlow(0)
//
//        override fun translate(phraseKey: String) = phraseKey
//
//        override fun translate(phraseKey: String, fallbackResId: Int) = phraseKey
//    }
//)

val passThroughTranslator = object : KeyResourceTranslator {
    override val translationCount = MutableStateFlow(0)

    override fun translate(phraseKey: String) = phraseKey

    override fun translate(phraseKey: String, fallbackResId: Int) = phraseKey
}

val LocalAppTranslator = staticCompositionLocalOf { passThroughTranslator }