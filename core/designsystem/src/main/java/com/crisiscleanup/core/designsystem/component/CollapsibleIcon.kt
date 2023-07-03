package com.crisiscleanup.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.crisiscleanup.core.designsystem.LocalAppTranslator


@Composable
fun CollapsibleIcon(
    isCollapsed: Boolean,
    sectionTitle: String,
    iconVector: ImageVector,
) {
    val translator = LocalAppTranslator.current.translator
    val translateKey = if (isCollapsed) "actions.collapse_section"
    else "actions.expand_section"
    val description = translator(translateKey)
        .replace("{section}", sectionTitle)
    Icon(
        imageVector = iconVector,
        contentDescription = description,
    )
}
