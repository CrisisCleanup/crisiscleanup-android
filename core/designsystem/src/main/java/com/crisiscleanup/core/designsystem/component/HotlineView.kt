package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier

@Composable
fun HotlineHeaderView() {
    Text(
        LocalAppTranslator.current("~~Hotline:"),
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            .then(listItemModifier),
        style = LocalFontStyles.current.header2,
    )
}

@Composable
fun HotlineIncidentView(
    name: String,
    activePhoneNumbers: List<String>,
) {
    val phoneNumbers = activePhoneNumbers.combineTrimText("\n")
    Text(
        "$name: $phoneNumbers",
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            .then(listItemModifier),
        style = LocalFontStyles.current.header4,
    )
}