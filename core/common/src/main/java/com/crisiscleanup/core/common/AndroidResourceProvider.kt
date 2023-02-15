package com.crisiscleanup.core.common

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AndroidResourceProvider {
    val displayDensity: Float

    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String

    fun dpToPx(dp: Float): Float
}

@Singleton
class ApplicationResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AndroidResourceProvider {
    val resources: Resources = context.resources

    override val displayDensity: Float = resources.displayMetrics.density

    override fun getString(@StringRes resId: Int): String = context.getString(resId)
    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        resources.getString(resId, *formatArgs)

    override fun dpToPx(dp: Float): Float = displayDensity * dp
}