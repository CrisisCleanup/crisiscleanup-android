package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.R
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.common.R as commonR

@Composable
fun CrisisCleanupLogoRow(
    hideHeaderText: Boolean = false,
) {
    // TODO Adjust to other screen sizes as necessary
    Box(Modifier.padding(top = 16.dp, start = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Image(
                painterResource(R.drawable.worker_wheelbarrow_world_background),
                modifier = Modifier
                    .testTag("ccuBackground")
                    .padding(top = 32.dp)
                    .size(width = 480.dp, height = 240.dp)
                    .offset(x = 64.dp),
                contentScale = ContentScale.FillHeight,
                contentDescription = null,
            )
        }
        if (!hideHeaderText) {
            Row(
                modifier = fillWidthPadded,
                horizontalArrangement = Arrangement.Start,
            ) {
                Image(
                    modifier = Modifier
                        .testTag("ccuLogo")
                        .sizeIn(maxWidth = 160.dp),
                    painter = painterResource(commonR.drawable.crisis_cleanup_logo),
                    contentDescription = stringResource(commonR.string.crisis_cleanup),
                )
            }
        }
    }
}
