package com.crisiscleanup.sandbox.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.crisiscleanup.core.designsystem.component.CrisisCleanupNavigationDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.crisiscleanup.feature.caseeditor.R as caseeditorR

@Composable
fun BottomNavRoute() {
    Column {
        var enabled by remember { mutableStateOf(false) }
        val contentColor = if (enabled) Color.Black else Color.Black.copy(alpha = 0.5f)
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                delay(1000)
                enabled = true
            }
        }

        NavigationBar(
            containerColor = Color.White,
            contentColor = contentColor,
        ) {
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = {
                    Icon(
                        painter = painterResource(caseeditorR.drawable.ic_flag_small),
                        contentDescription = null,
                        tint = contentColor,
                    )
                },
                label = { Text("Nav") },
                colors = NavigationBarItemDefaults.colors(
                    unselectedIconColor = contentColor,
                    unselectedTextColor = contentColor,
                    indicatorColor = CrisisCleanupNavigationDefaults.navigationIndicatorColor(),
                ),
                enabled = enabled,
            )
        }
    }
}
