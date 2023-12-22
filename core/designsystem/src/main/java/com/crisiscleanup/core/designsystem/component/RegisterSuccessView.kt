package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.statusClosedColor

@Composable
fun RegisterSuccessView(
    title: String,
    text: String,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RegisterSuccessView(
            title = title,
            text = text,
        )
    }
}

@Composable
fun ColumnScope.RegisterSuccessView(
    title: String,
    text: String,
) {
    Spacer(Modifier.weight(1f))

    Icon(
        modifier = Modifier.size(64.dp),
        imageVector = CrisisCleanupIcons.CheckCircle,
        contentDescription = null,
        tint = statusClosedColor,
    )

    androidx.compose.material3.Text(
        title,
        listItemModifier,
        style = LocalFontStyles.current.header1,
        textAlign = TextAlign.Center,
    )

    androidx.compose.material3.Text(
        text,
        listItemModifier,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.weight(1f))

    CrisisCleanupLogoRow(true)

    Spacer(Modifier.weight(1f))
}
