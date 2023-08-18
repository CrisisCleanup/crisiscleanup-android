package com.crisiscleanup.ui

import android.webkit.URLUtil
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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

@Composable
internal fun EndOfLifeView(
    endOfLife: BuildEndOfLife,
) {
    Column(
        listItemModifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = com.crisiscleanup.core.common.R.drawable.crisis_cleanup_logo),
            contentDescription = null,
        )

        Spacer(Modifier.height(24.dp))

        if (endOfLife.title.isNotBlank()) {
            Text(
                endOfLife.title,
                Modifier.padding(vertical = 8.dp),
                style = LocalFontStyles.current.header2,
            )
        }

        Text(
            text = endOfLife.message,
            Modifier.padding(vertical = 8.dp),
        )

        if (URLUtil.isValidUrl(endOfLife.link)) {
            val uriHandler = LocalUriHandler.current
            Text(
                text = endOfLife.link,
                Modifier
                    .padding(vertical = 8.dp)
                    .clickable { uriHandler.openUri(endOfLife.link) },
            )
        }
    }
}