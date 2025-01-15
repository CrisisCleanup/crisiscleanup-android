package com.crisiscleanup.sandbox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.sandbox.MapMarkersViewModel

@Composable
fun MapMarkersView(
    viewModel: MapMarkersViewModel = hiltViewModel(),
) {
    val markers by viewModel.mapMarkers.collectAsStateWithLifecycle()
    if (markers.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            BusyIndicatorFloatingTopCenter(true)
        }
    } else {
        LazyVerticalStaggeredGrid(
            StaggeredGridCells.Fixed(4),
            Modifier
                .background(Color.White)
                .fillMaxSize(),
            verticalItemSpacing = 4.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                markers,
            ) {
                if (it == null) {
                    Text(
                        "?",
                        style = MaterialTheme.typography.titleLarge,
                    )
                } else {
                    Image(
                        it.asImageBitmap(),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
