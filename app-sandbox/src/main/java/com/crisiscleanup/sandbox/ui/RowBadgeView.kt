package com.crisiscleanup.sandbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun RowScope.BadgedText(
    alignment: Alignment,
    text: String,
) {
    BadgedBox(
        {
            Badge(
                Modifier
                    .align(alignment)
                    .size(20.dp),
                containerColor = Color.Red,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                )
            }
        },
        Modifier
            .background(Color.LightGray)
            .weight(1f),
    ) {
        Text(
            text,
            Modifier
                .align(Alignment.CenterStart)
                .background(Color.Yellow),
        )
    }
}

@Composable
private fun RowBadge(
    alignment: Alignment,
    text: String,
    buttonText: String = "Press me",
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BadgedText(alignment, text)
        Button({}) {
            Text(buttonText)
        }
    }
}

@Composable
private fun ReverseRowBadge(
    alignment: Alignment,
    text: String,
    buttonText: String = "Press me",
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button({}) {
            Text(buttonText)
        }
        BadgedText(alignment, text)
    }
}

@Composable
fun RowBadgeView() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        RowBadge(Alignment.TopStart, "Top start")
        RowBadge(Alignment.TopCenter, "Top center")
        RowBadge(Alignment.TopEnd, "Top end")
        RowBadge(Alignment.BottomEnd, "Bottom end")
        RowBadge(Alignment.BottomCenter, "Bottom center")
        RowBadge(Alignment.BottomStart, "Bottom start")
        ReverseRowBadge(Alignment.TopStart, "Top start")
        ReverseRowBadge(Alignment.TopCenter, "Top center")
        ReverseRowBadge(Alignment.TopEnd, "Top end")
        ReverseRowBadge(Alignment.BottomEnd, "Bottom end")
        ReverseRowBadge(Alignment.BottomCenter, "Bottom center")
        ReverseRowBadge(Alignment.BottomStart, "Bottom start")
    }
}
