package com.crisiscleanup.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
fun CollapsibleIcon(
    isCollapsed: Boolean,
    sectionTitle: String,
    testTagPostfix: String = "",
) {
    val iconVector = if (isCollapsed) {
        CrisisCleanupIcons.ExpandLess
    } else {
        CrisisCleanupIcons.ExpandMore
    }
    val translator = LocalAppTranslator.current
    val translateKey = if (isCollapsed) {
        "actions.collapse_section"
    } else {
        "actions.expand_section"
    }
    val description = translator(translateKey)
        .replace("{section}", sectionTitle)
    Icon(
        imageVector = iconVector,
        contentDescription = description,
        modifier = Modifier.testTag("collapsibleIcon$testTagPostfix"),
    )
}
