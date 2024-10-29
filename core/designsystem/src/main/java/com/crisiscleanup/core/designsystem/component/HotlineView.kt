package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy

@Composable
fun HotlineHeaderView(
    isExpanded: Boolean,
    toggleExpandHotline: () -> Unit,
) {
    val hotlineText = LocalAppTranslator.current("disasters.hotline")
    Row(
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = toggleExpandHotline)
            .then(listItemModifier),
        horizontalArrangement = listItemSpacedBy,
    ) {
        Text(
            hotlineText,
            Modifier.weight(1f),
            style = LocalFontStyles.current.header2,
        )
        CollapsibleIcon(!isExpanded, hotlineText)
    }
}

@Composable
fun HotlineIncidentView(
    name: String,
    activePhoneNumbers: List<String>,
    linkifyNumbers: Boolean = false,
) {
    val phoneNumbers = activePhoneNumbers.combineTrimText(", ")
    val text = "$name: $phoneNumbers"
    val modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        .then(listItemModifier)
    val style = LocalFontStyles.current.header4
    if (linkifyNumbers) {
        LinkifyPhoneText(
            text,
            modifier,
            com.crisiscleanup.core.designsystem.R.style.link_text_style_black,
        )
    } else {
        Text(
            text,
            modifier,
            style = style,
        )
    }
}