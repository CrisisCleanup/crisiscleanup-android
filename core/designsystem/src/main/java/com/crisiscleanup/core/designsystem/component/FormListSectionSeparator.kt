package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.theme.separatorColor

@Composable
fun FormListSectionSeparator() {
    Box(
        Modifier
            .fillMaxWidth()
            // TODO Common dimensions
            .height(32.dp)
            .background(color = separatorColor)
    )
}
