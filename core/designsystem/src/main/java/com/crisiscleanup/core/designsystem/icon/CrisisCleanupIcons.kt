package com.crisiscleanup.core.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.crisiscleanup.core.designsystem.R

/**
 * Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */
object CrisisCleanupIcons {
    val Account = Icons.Default.PersonOutline
    val Add = Icons.Default.Add
    val AddNote = Icons.Default.NoteAdd
    val ArrowDropDown = Icons.Default.ArrowDropDown
    val Cases = R.drawable.ic_cases
    val Check = Icons.Default.Check
    val Clear = Icons.Default.Clear
    val CloudOff = Icons.Default.CloudOff
    val Dashboard = R.drawable.ic_dashboard
    val Edit = Icons.Default.Edit
    val ExpandLess = Icons.Default.ExpandLess
    val ExpandMore = Icons.Default.ExpandMore
    val Help = Icons.Default.HelpOutline
    val Location = Icons.Default.LocationOn
    val Menu = Icons.Default.Menu
    val MoreVert = Icons.Default.MoreVert
    val MyLocation = Icons.Default.MyLocation
    val Search = Icons.Rounded.Search
    val Team = R.drawable.ic_team
    val UnfoldMore = Icons.Default.UnfoldMore
    val Visibility = Icons.Default.Visibility
    val VisibilityOff = Icons.Default.VisibilityOff
}

/**
 * A sealed class to make dealing with [ImageVector] and [DrawableRes] icons easier.
 */
sealed class Icon {
    data class ImageVectorIcon(val imageVector: ImageVector) : Icon()
    data class DrawableResourceIcon(@DrawableRes val id: Int) : Icon()
}
