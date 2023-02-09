package com.crisiscleanup.core.common

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AndroidResourceProvider {
    fun getString(@StringRes resId: Int): String

    fun dpToPx(dp: Float): Float
}

@Singleton
class ApplicationResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : AndroidResourceProvider {
    val resources: Resources = context.resources

    override fun getString(resId: Int): String = context.getString(resId)

    override fun dpToPx(dp: Float): Float = resources.displayMetrics.density * dp
}