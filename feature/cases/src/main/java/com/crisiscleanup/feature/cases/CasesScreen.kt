package com.crisiscleanup.feature.cases

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.feature.cases.ui.CasesAction
import com.crisiscleanup.feature.cases.ui.CasesActionBar
import com.crisiscleanup.feature.cases.ui.CasesZoomBar
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
internal fun CasesRoute(
    modifier: Modifier = Modifier,
    viewModel: CasesViewModel = hiltViewModel()
) {
    CasesScreen(
        modifier = modifier
    )
}

@Composable
internal fun CasesScreen(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    onNewCase: () -> Unit = {},
    isTableView: Boolean = false,
    latLngInitial: LatLng = LatLng(40.272621, -96.012327),
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLngInitial, 10f)
    }
    Box(modifier.then(Modifier.fillMaxSize())) {
        if (isTableView) {
            Text("Show table view")
        } else {
            // TODO rememberSaveable (for configuration changes) requires saver. Research or write.
            //      Reference rememberCameraPositionState or similar.
            val uiSettings by remember {
                mutableStateOf(
                    MapUiSettings(
                        zoomControlsEnabled = false,
                    )
                )
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                uiSettings = uiSettings,
                cameraPositionState = cameraPositionState,
            )
        }
        CasesOverlayActions(
            modifier,
            onCasesAction,
            onNewCase,
        )
    }
}

private val actionSpacing = 8.dp
private val fabSpacing = 16.dp

@Composable
internal fun CasesOverlayActions(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    onNewCase: () -> Unit = {},
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (zoomBar, actionBar, newCaseFab) = createRefs()

        CasesZoomBar(
            modifier.constrainAs(zoomBar) {
                top.linkTo(parent.top, margin = actionSpacing)
                start.linkTo(parent.start, margin = actionSpacing)
            },
            onCasesAction,
        )

        CasesActionBar(
            modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = actionSpacing)
                end.linkTo(parent.end, margin = actionSpacing)
            },
            onCasesAction,
        )

        FloatingActionButton(
            modifier = modifier.constrainAs(newCaseFab) {
                end.linkTo(parent.end, margin = fabSpacing)
                bottom.linkTo(parent.bottom, margin = fabSpacing)
            },
            onClick = onNewCase,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.create_case),
            )
        }
    }
}

@Preview
@Composable
fun CasesOverlayActionsPreview() {
    CasesOverlayActions()
}