package com.crisiscleanup.feature.cases.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import com.crisiscleanup.core.designsystem.component.*
import com.crisiscleanup.feature.cases.R

enum class CasesAction(
    val iconResId: Int,
    val contentDescriptionResId: Int,
) {
    CreateNew(android.R.drawable.btn_plus, R.string.create_case),
    Search(R.drawable.ic_search, R.string.search),
    TableView(R.drawable.ic_table, R.string.table_view),
    Filters(R.drawable.ic_dials, R.string.filters),
    Layers(R.drawable.ic_layers, R.string.layers),
    MapView(R.drawable.ic_map, R.string.map_view),
    ZoomToInteractive(R.drawable.ic_zoom_interactive, R.string.zoom_to_interactive),
    ZoomToIncident(R.drawable.ic_zoom_incident, R.string.zoom_to_incident),
    ZoomIn(R.drawable.ic_plus, R.string.zoom_in),
    ZoomOut(R.drawable.ic_minus, R.string.zoom_out),
}

@Composable
private fun CasesActionButton(
    modifier: Modifier = Modifier,
    action: CasesAction,
    onCasesAction: (CasesAction) -> Unit,
    shape: Shape = actionRoundCornerShape,
) {
    CrisisCleanupIconButton(
        modifier = modifier.actionSmallSize(),
        iconResId = action.iconResId,
        contentDescriptionResId = action.contentDescriptionResId,
        onClick = { onCasesAction(action) },
        shape = shape,
    )
}

@Composable
internal fun CasesActionBar(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(adjacentButtonSpace),
    ) {
        CasesActionButton(
            modifier, CasesAction.Search, onCasesAction, shape = actionStartRoundCornerShape
        )
        CasesActionButton(
            modifier,
            CasesAction.Filters,
            onCasesAction,
            shape = actionEndRoundCornerShape,
        )
    }
}

@Composable
internal fun CasesZoomBar(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(actionSmallSpace),
    ) {
        Column(
            modifier,
            verticalArrangement = Arrangement.spacedBy(adjacentButtonSpace),
        ) {
            CasesActionButton(
                modifier,
                CasesAction.ZoomIn,
                onCasesAction,
                shape = actionTopRoundCornerShape,
            )
            CasesActionButton(
                modifier,
                CasesAction.ZoomOut,
                onCasesAction,
                shape = actionBottomRoundCornerShape,
            )
        }
        CasesActionButton(modifier, CasesAction.ZoomToInteractive, onCasesAction)
        CasesActionButton(modifier, CasesAction.ZoomToIncident, onCasesAction)
        CasesActionButton(modifier, CasesAction.Layers, onCasesAction)
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES, name = "Toggle to table")
@Composable
fun CasesActionBarPreview() {
    CasesActionBar()
}

@Preview
@Composable
fun CasesZoomBarPreview() {
    CasesZoomBar()
}