package com.crisiscleanup.feature.cases.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
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
}

private val actionSize = 48.dp
private val zoomBarSpacing = 8.dp

@Composable
private fun CasesActionButton(
    modifier: Modifier = Modifier,
    action: CasesAction,
    onCasesAction: (CasesAction) -> Unit,
) {
    CrisisCleanupIconButton(
        modifier = modifier.size(actionSize),
        iconResId = action.iconResId,
        contentDescriptionResId = action.contentDescriptionResId,
        onClick = { onCasesAction(action) },
    )
}

@Composable
internal fun CasesActionBar(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    isTableView: Boolean = false,
) {
    Row(
        modifier,
        // TODO Move value into common
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        CasesActionButton(modifier, CasesAction.Search, onCasesAction)
        val tableMapAction = if (isTableView) CasesAction.MapView else CasesAction.TableView
        CasesActionButton(modifier, tableMapAction, onCasesAction)
        CasesActionButton(modifier, CasesAction.Filters, onCasesAction)
        if (isTableView) {
            Spacer(modifier = Modifier.size(actionSize))
        } else {
            CasesActionButton(modifier, CasesAction.Layers, onCasesAction)
        }
    }
}

@Composable
internal fun CasesZoomBar(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(zoomBarSpacing),
    ) {
        CasesActionButton(modifier, CasesAction.ZoomToInteractive, onCasesAction)
        CasesActionButton(modifier, CasesAction.ZoomToIncident, onCasesAction)
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES, name = "Toggle to table")
@Composable
fun CasesActionBarPreview() {
    CasesActionBar()
}

@Preview(name = "Toggle to map")
@Composable
fun CasesActionBarTableViewPreview() {
    CasesActionBar(isTableView = true)
}

@Preview
@Composable
fun CasesZoomBarPreview() {
    CasesZoomBar()
}