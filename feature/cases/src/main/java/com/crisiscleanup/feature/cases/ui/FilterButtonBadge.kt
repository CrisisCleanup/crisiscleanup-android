package com.crisiscleanup.feature.cases.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterButtonBadge(
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
