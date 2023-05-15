package com.crisiscleanup.core.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    val Clear = Icons.Default.Clear
    val CloudSync = Icons.Default.CloudSync
    val CloudOff = Icons.Default.CloudOff
    val Dashboard = R.drawable.ic_dashboard
    val Edit = Icons.Default.Edit
    val ExpandLess = Icons.Default.ExpandLess
    val ExpandMore = Icons.Default.ExpandMore
    val Help = Icons.Default.HelpOutline
    val Location = Icons.Default.LocationOn
    val Mail = Icons.Default.Mail
    val Menu = Icons.Default.Menu
    val MoreVert = Icons.Default.MoreVert
    val MyLocation = Icons.Default.MyLocation
    val Person = Icons.Default.Person
    val Phone = Icons.Default.Phone
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
