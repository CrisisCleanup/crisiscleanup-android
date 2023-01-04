package com.crisiscleanup.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.crisiscleanup.core.designsystem.R

/**
 * Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */
object CrisisCleanupIcons {
    val Cases = R.drawable.ic_cases
    val Dashboard = R.drawable.ic_dashboard
    val Team = R.drawable.ic_team
}

/**
 * A sealed class to make dealing with [ImageVector] and [DrawableRes] icons easier.
 */
sealed class Icon {
    data class ImageVectorIcon(val imageVector: ImageVector) : Icon()
    data class DrawableResourceIcon(@DrawableRes val id: Int) : Icon()
}
