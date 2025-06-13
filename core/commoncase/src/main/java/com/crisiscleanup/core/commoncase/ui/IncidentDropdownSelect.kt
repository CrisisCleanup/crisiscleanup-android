package com.crisiscleanup.core.commoncase.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.commonassets.DisasterIcon
import com.crisiscleanup.core.designsystem.component.TruncatedAppBarText
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

@Composable
fun IncidentDropdownSelect(
    modifier: Modifier = Modifier,
    onOpenIncidents: () -> Unit = {},
    @DrawableRes disasterIconResId: Int = 0,
    title: String = "",
    contentDescription: String = "",
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.clickable(
            onClick = onOpenIncidents,
            enabled = enabled,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val contentColor = LocalContentColor.current
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) contentColor else contentColor.disabledAlpha(),
        ) {
            DisasterIcon(disasterIconResId, title, enabled)
            TruncatedAppBarText(
                title = title,
                modifier = Modifier.padding(start = 8.dp),
            )
            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = contentDescription,
            )
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CircularProgressIndicator(
                    modifier
                        .testTag("incidentSelectLoadingIndicator")
                        .size(48.dp)
                        .padding(8.dp),
                )
            }
        }
    }
}
