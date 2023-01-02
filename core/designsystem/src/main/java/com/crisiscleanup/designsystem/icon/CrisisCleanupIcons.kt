package com.crisiscleanup.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.crisiscleanup.core.designsystem.R

/**
 * Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */
object CrisisCleanupIcons {
//    val AccountCircle = Icons.Outlined.AccountCircle
//    val Add = Icons.Rounded.Add
//    val ArrowBack = Icons.Rounded.ArrowBack
//    val ArrowDropDown = Icons.Default.ArrowDropDown
//    val ArrowDropUp = Icons.Default.ArrowDropUp
    val Cases = R.drawable.ic_cases
    val Dashboard = R.drawable.ic_dashboard
//    val Check = Icons.Rounded.Check
//    val Close = Icons.Rounded.Close
//    val ExpandLess = Icons.Rounded.ExpandLess
//    val Fullscreen = Icons.Rounded.Fullscreen
//    val Grid3x3 = Icons.Rounded.Grid3x3
//    val MoreVert = Icons.Default.MoreVert
//    val Person = Icons.Rounded.Person
//    val PlayArrow = Icons.Rounded.PlayArrow
//    val Search = Icons.Rounded.Search
//    val Settings = Icons.Rounded.Settings
//    val ShortText = Icons.Rounded.ShortText
//    val Tag = Icons.Rounded.Tag
    val Team = R.drawable.ic_team
//    val ViewDay = Icons.Rounded.ViewDay
//    val VolumeOff = Icons.Rounded.VolumeOff
//    val VolumeUp = Icons.Rounded.VolumeUp
}

/**
 * A sealed class to make dealing with [ImageVector] and [DrawableRes] icons easier.
 */
sealed class Icon {
    data class ImageVectorIcon(val imageVector: ImageVector) : Icon()
    data class DrawableResourceIcon(@DrawableRes val id: Int) : Icon()
}
