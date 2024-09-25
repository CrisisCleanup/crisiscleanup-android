package com.crisiscleanup.core.commoncase.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.navigationContainerColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CaseMapCountView(
    countText: String,
    isLoadingData: Boolean,
    modifier: Modifier = Modifier,
    onSyncDataDelta: () -> Unit = {},
    onSyncDataFull: () -> Unit = {},
) {
    // TODO Common dimensions of elements
    Surface(
        modifier,
        color = navigationContainerColor,
        contentColor = Color.White,
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(
            Modifier
                .combinedClickable(
                    enabled = !isLoadingData,
                    onClick = onSyncDataDelta,
                    onLongClick = onSyncDataFull,
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = listItemSpacedBy,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = countText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(countText)
            }

            AnimatedVisibility(
                visible = isLoadingData,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CircularProgressIndicator(
                    Modifier
                        .testTag("workIncidentsLoadingIndicator")
                        .wrapContentSize()
                        .size(24.dp),
                    color = Color.White,
                )
            }
        }
    }
}
