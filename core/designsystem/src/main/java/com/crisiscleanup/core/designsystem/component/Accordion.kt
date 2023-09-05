package com.crisiscleanup.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemPadding

@Composable
fun Accordion(
    title: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initiallyCollapsed: Boolean = true,
) {
    var isCollapsed by remember { mutableStateOf(initiallyCollapsed) }
    val rotation: Float by animateFloatAsState(
        if (!isCollapsed) 180f else 0f,
        label = "rotation",
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isCollapsed = !isCollapsed }
                .listItemHeight()
                .listItemPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Text(
                text = title,
                modifier = Modifier.testTag("accordionTitle_${title}"),
                style = LocalFontStyles.current.header3,
            )
            Icon(
                imageVector = CrisisCleanupIcons.ExpandLess,
                contentDescription = "Expand/Collapse",
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = !isCollapsed) {
            Column(modifier = Modifier.animateContentSize()) {
                content()
            }
        }
    }
}
