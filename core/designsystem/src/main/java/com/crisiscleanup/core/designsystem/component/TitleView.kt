package com.crisiscleanup.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles

@Composable
fun HeaderTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        style = LocalFontStyles.current.header3,
        modifier = modifier,
    )
}

@Composable
fun HeaderSubTitle(
    subTitle: String,
    modifier: Modifier = Modifier,
) {
    if (subTitle.isNotBlank()) {
        Text(
            subTitle,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier,
        )
    }
}
