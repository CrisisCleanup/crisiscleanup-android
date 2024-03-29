package com.crisiscleanup.core.common

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AndroidResourceProvider {
    val resources: Resources

    val displayDensity: Float

    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String

    fun dpToPx(dp: Float): Float

    fun getDrawable(@DrawableRes drawableId: Int, theme: Resources.Theme? = null): Drawable
}

@Singleton
class ApplicationResourceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AndroidResourceProvider {
    override val resources: Resources = context.resources

    override val displayDensity: Float = resources.displayMetrics.density

    override fun getString(@StringRes resId: Int) = context.getString(resId)
    override fun getString(@StringRes resId: Int, vararg formatArgs: Any) =
        resources.getString(resId, *formatArgs)

    override fun dpToPx(dp: Float) = displayDensity * dp

    override fun getDrawable(drawableId: Int, theme: Resources.Theme?): Drawable =
        resources.getDrawable(drawableId, theme)
}
