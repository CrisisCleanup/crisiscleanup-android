package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.separatorColor

@Composable
fun FormListSectionSeparator() {
    Divider(
        Modifier.fillMaxWidth(),
        // TODO Common dimensions
        thickness = 32.dp,
        color = separatorColor,
    )
}

@Composable
fun LineDivider() {
    Divider(
        Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = separatorColor,
    )
}
