package com.crisiscleanup.sandbox.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.crisiscleanup.core.designsystem.component.LeadingIconChip

@Composable
fun ChipsRoute() {
    Column {
        LeadingIconChip(
            text = "label",
            onIconClick = {},
            isEditable = true,
            containerColor = Color(0xFF00b3bf),
            contentTint = Color.White,
        )
    }
}
