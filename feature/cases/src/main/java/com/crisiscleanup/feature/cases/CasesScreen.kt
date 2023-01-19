package com.crisiscleanup.feature.cases

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
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
    onCasesAction: (CasesAction) -> Unit = {},
    onNewCase: () -> Unit = {},
) {
    var isTableView by rememberSaveable { mutableStateOf(false) }

    val rememberOnCasesAction = remember(onCasesAction) {
        { action: CasesAction ->
            onCasesAction(action)
            if (isTableView) {
                if (action == CasesAction.MapView) {
                    isTableView = false
                }
            } else {
                if (action == CasesAction.TableView) {
                    isTableView = true
                }
            }
        }
    }
    val rememberOnNewCase = remember(onNewCase) { { onNewCase() } }
    CasesScreen(
        modifier = modifier,
        rememberOnCasesAction,
        rememberOnNewCase,
        isTableView,
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
    Box(modifier.then(Modifier.fillMaxSize())) {
        if (isTableView) {
            Text("Table view", Modifier.padding(48.dp))
        } else {
            CasesMapView(latLngInitial)
        }
        CasesOverlayActions(
            modifier,
            onCasesAction,
            isTableView,
            onNewCase,
        )
    }
}

@Composable
internal fun CasesMapView(
    latLngInitial: LatLng,
) {
    // rememberSaveable (for configuration changes) requires saver. Research or write.
    // Reference rememberCameraPositionState or similar.
    val uiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
            )
        )
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLngInitial, 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        uiSettings = uiSettings,
        cameraPositionState = cameraPositionState,
    )
}

private val actionSpacing = 8.dp
private val fabSpacing = 16.dp

@Composable
internal fun CasesOverlayActions(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
    onNewCase: () -> Unit = {},
) {
    ConstraintLayout(Modifier.fillMaxSize()) {
        val (zoomBar, actionBar, newCaseFab) = createRefs()

        if (!isTableView) {
            CasesZoomBar(
                modifier.constrainAs(zoomBar) {
                    top.linkTo(parent.top, margin = actionSpacing)
                    start.linkTo(parent.start, margin = actionSpacing)
                },
                onCasesAction,
            )
        }

        CasesActionBar(
            modifier.constrainAs(actionBar) {
                top.linkTo(parent.top, margin = actionSpacing)
                end.linkTo(parent.end, margin = actionSpacing)
            },
            onCasesAction,
            isTableView,
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