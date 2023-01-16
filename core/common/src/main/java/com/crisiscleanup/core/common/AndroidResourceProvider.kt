package com.crisiscleanup.core.common

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AndroidResourceProvider {
    fun getString(@StringRes resId: Int): String
}

@Singleton
class ApplicationResourceProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) : AndroidResourceProvider {
    override fun getString(resId: Int): String = applicationContext.getString(resId)
}