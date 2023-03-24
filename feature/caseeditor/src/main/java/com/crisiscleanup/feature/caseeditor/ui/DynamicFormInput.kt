package com.crisiscleanup.feature.caseeditor.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox

internal fun LazyListScope.checkboxListItem(
    itemKey: String,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    @StringRes textResId: Int = 0,
    text: String = "",
    onToggle: () -> Unit = {},
    onCheckChange: (Boolean) -> Unit = {},
) {
    item(
        key = itemKey,
        contentType = "checkbox",
    ) {
        CrisisCleanupTextCheckbox(
            modifier,
            checked,
            textResId,
            text,
            onToggle,
            onCheckChange,
        )
    }
}