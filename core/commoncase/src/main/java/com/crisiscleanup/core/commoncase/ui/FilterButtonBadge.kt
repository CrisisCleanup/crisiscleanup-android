package com.crisiscleanup.core.commoncase.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor

@Composable
fun FilterButtonBadge(
    filtersCount: Int = 0,
    content: @Composable () -> Unit,
) {
    if (filtersCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    // TODO Common/relative dimensions
                    Modifier
                        .offset(((-8)).dp, 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "$filtersCount",
                        modifier = Modifier.testTag("filterButtonBadge_$filtersCount"),
                    )
                }
            },
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun OverlayBadge(
    count: Int = 0,
    color: Color = primaryBlueColor,
    contentColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    if (count > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = color) {
                    CompositionLocalProvider(
                        LocalContentColor provides contentColor,
                    ) {
                        Text("$count")
                    }
                }
            },
        ) {
            content()
        }
    } else {
        content()
    }
}
