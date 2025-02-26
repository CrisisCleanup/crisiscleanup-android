package com.crisiscleanup.core.designsystem.icon

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.crisiscleanup.core.designsystem.R

private val icons = Icons.Default

/**
 * Material icons are [ImageVector]s, custom icons are drawable resource IDs.
 */
object CrisisCleanupIcons {
    val Account = icons.PersonOutline
    val Add = icons.Add
    val ArrowBack = Icons.AutoMirrored.Filled.ArrowBackIos
    val ArrowBack2 = Icons.AutoMirrored.Filled.ArrowBack
    val ArrowDropDown = icons.ArrowDropDown
    val Calendar = icons.CalendarMonth
    val CaretUp = icons.KeyboardArrowUp
    val Cases = R.drawable.ic_cases
    val Check = icons.Check
    val CheckCircle = icons.CheckCircle
    val Clear = icons.Clear
    val CloudSync = icons.CloudSync
    val Cloud = icons.Cloud
    val Close = icons.Close
    val Dashboard = R.drawable.ic_dashboard
    val Delete = icons.Delete
    val Directions = icons.Directions
    val Edit = icons.Edit
    val ExpandAll = icons.UnfoldMore
    val ExpandLess = icons.ExpandLess
    val ExpandMore = icons.ExpandMore
    val File = icons.Description
    val Help = Icons.AutoMirrored.Filled.HelpOutline
    val Info = icons.Info
    val List = Icons.AutoMirrored.Filled.List
    val Location = icons.LocationOn
    val Mail = icons.Mail
    val Minus = icons.Remove
    val Menu = icons.Menu
    val MissingAvatar = icons.SentimentNeutral
    val MoreVert = icons.MoreVert
    val MyLocation = icons.MyLocation
    val Organization = icons.Domain
    val PanMap = icons.OpenWith
    val Person = icons.Person
    val PendingRequestRedeploy = icons.Schedule
    val Phone = icons.Phone
    val PhotoGrid = icons.PhotoLibrary
    val Play = icons.PlayArrow
    val QrCode = icons.QrCode
    val QuestionMark = icons.QuestionMark
    val RotateClockwise = icons.Rotate90DegreesCw
    val RotateCcw = icons.Rotate90DegreesCcw
    val Search = Icons.Rounded.Search
    val Team = R.drawable.ic_team
    val UnfoldMore = icons.UnfoldMore
    val Visibility = icons.Visibility
    val VisibilityOff = icons.VisibilityOff
    val Warning = icons.Warning
}

/**
 * A sealed class to make dealing with [ImageVector] and [DrawableRes] icons easier.
 */
sealed class Icon {
    data class ImageVectorIcon(val imageVector: ImageVector) : Icon()
    data class DrawableResourceIcon(@DrawableRes val id: Int) : Icon()
}
