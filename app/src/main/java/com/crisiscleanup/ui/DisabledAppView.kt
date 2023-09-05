package com.crisiscleanup.ui

import android.webkit.URLUtil
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.model.data.BuildEndOfLife
import com.crisiscleanup.core.model.data.MinSupportedAppVersion

@Composable
private fun DisabledAppView(
    title: String,
    message: String,
    link: String,
) {
    Column(
        listItemModifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.fillMaxWidth(0.67f)) {
            Image(
                painter = painterResource(id = com.crisiscleanup.core.common.R.drawable.crisis_cleanup_logo),
                contentDescription = null,
            )
        }

        Spacer(Modifier.height(24.dp))

        if (title.isNotBlank()) {
            Text(
                title,
                Modifier.padding(vertical = 8.dp),
                style = LocalFontStyles.current.header2,
            )
        }

        Text(
            text = message,
            Modifier.padding(vertical = 8.dp),
        )

        if (URLUtil.isValidUrl(link)) {
            val uriHandler = LocalUriHandler.current
            Text(
                text = link,
                Modifier
                    .padding(vertical = 8.dp)
                    .clickable { uriHandler.openUri(link) },
            )
        }
    }
}

@Composable
internal fun EndOfLifeView(
    endOfLife: BuildEndOfLife,
) {
    DisabledAppView(
        title = endOfLife.title,
        message = endOfLife.message,
        link = endOfLife.link,
    )
}

@Composable
internal fun UnsupportedBuildView(
    supportedVersion: MinSupportedAppVersion,
) {
    DisabledAppView(
        title = supportedVersion.title,
        message = supportedVersion.message,
        link = supportedVersion.link,
    )
}
