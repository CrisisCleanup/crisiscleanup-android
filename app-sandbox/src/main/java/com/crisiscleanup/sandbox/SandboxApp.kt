package com.crisiscleanup.sandbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.component.CrisisCleanupBackground
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox

@Composable
fun SandboxApp(
    windowSizeClass: WindowSizeClass,
) {
    CrisisCleanupBackground {
        Box(Modifier.fillMaxSize()) {
            val snackbarHostState = remember { SnackbarHostState() }

            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {},
                bottomBar = {},
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Show")

                    Checkboxes()
                }
            }
        }
    }
}

@Composable
private fun Checkboxes() {
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