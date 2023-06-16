package com.crisiscleanup.feature.caseeditor.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyButton
import com.crisiscleanup.core.designsystem.component.cancelButtonColors
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemTopPadding

@Composable
internal fun CaseStaticText(
    text: String,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
) {
    Text(
        text,
        modifier,
        fontWeight = if (isBold) FontWeight.Bold else null,
    )
}

internal fun LazyListScope.textItem(
    text: String,
    modifier: Modifier = Modifier,
    isBold: Boolean = false,
) {
    item(contentType = "item-text") {
        CaseStaticText(text, modifier, isBold)
    }
}

internal fun LazyListScope.listTextItem(
    text: String,
    isBold: Boolean = false,
) {
    textItem(text, listItemModifier, isBold)
}

internal fun LazyListScope.labelTextItem(
    text: String,
    isBold: Boolean = false,
) {
    textItem(
        text,
        Modifier
            .listItemHorizontalPadding()
            .listItemTopPadding(),
        isBold,
    )
}

@Composable
internal fun TwoActionBar(
    onPositiveAction: () -> Unit = {},
    onCancel: () -> Unit = {},
    enabled: Boolean = false,
    enablePositive: Boolean = true,
    isBusy: Boolean = false,
    cancelTranslationKey: String = "actions.cancel",
    positiveTranslationKey: String = "actions.save",
) {
    val translator = LocalAppTranslator.current.translator
    Row(
        modifier = Modifier
            // TODO Common dimensions
            .padding(16.dp),
        horizontalArrangement = listItemSpacedBy,
    ) {
        BusyButton(
            Modifier.weight(1f),
            text = translator(cancelTranslationKey),
            enabled = enabled,
            onClick = onCancel,
            colors = cancelButtonColors(),
        )
        BusyButton(
            Modifier.weight(1f),
            text = translator(positiveTranslationKey),
            enabled = enabled && enablePositive,
            onClick = onPositiveAction,
            indicateBusy = isBusy,
        )
    }
}