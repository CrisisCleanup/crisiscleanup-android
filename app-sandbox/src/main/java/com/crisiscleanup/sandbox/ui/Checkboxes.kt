package com.crisiscleanup.sandbox.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox

@Composable
fun CheckboxesRoute() {
    Column {
        CrisisCleanupTextCheckbox(
            text = "Checkbox text and everything in between wrapping",
            //wrapText = true,
        ) {
            Text("Longer trailing content")
        }
        CrisisCleanupTextCheckbox(text = "Short") {
            Text("Longer trailing content")
        }
        CrisisCleanupTextCheckbox(
            text = "Short",
            spaceTrailingContent = true,
        ) {
            Text("Longer trailing content")
        }
    }
}
