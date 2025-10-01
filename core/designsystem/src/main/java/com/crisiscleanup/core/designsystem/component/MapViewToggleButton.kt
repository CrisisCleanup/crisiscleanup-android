package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha

@Composable
fun BoxScope.MapViewToggleButton(
    isSatelliteView: Boolean,
    onToggle: (Boolean) -> Unit,
    // TODO Common dimensions
    padding: Dp = 8.dp,
) {
    val mapViewIcon = if (isSatelliteView) {
        CrisisCleanupIcons.NormalMap
    } else {
        CrisisCleanupIcons.SatelliteMap
    }
    val actionDescription =
        LocalAppTranslator.current("~~Toggle map normal/satellite view")
    CrisisCleanupIconButton(
        Modifier
            .padding(padding)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.disabledAlpha())
            .align(Alignment.TopEnd),
        imageVector = mapViewIcon,
        contentDescription = actionDescription,
        onClick = {
            onToggle(!isSatelliteView)
        },
    )
}
