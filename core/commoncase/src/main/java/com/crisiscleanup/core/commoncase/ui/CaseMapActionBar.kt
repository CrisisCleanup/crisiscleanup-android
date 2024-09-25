package com.crisiscleanup.core.commoncase.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.crisiscleanup.core.commoncase.R
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupElevatedIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.actionBottomRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionEndRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionSmallSize
import com.crisiscleanup.core.designsystem.component.actionSmallSpace
import com.crisiscleanup.core.designsystem.component.actionStartRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionTopRoundCornerShape
import com.crisiscleanup.core.designsystem.component.adjacentButtonSpace

enum class CasesAction(
    val iconResId: Int,
    val descriptionTranslateKey: String,
    val testTag: String,
) {
    CreateNew(android.R.drawable.btn_plus, "nav.new_case", "workNewCaseBtn"),
    Search(R.drawable.ic_search, "actions.search", "workIncidentSearchBtn"),
    TableView(R.drawable.ic_table, "actions.table_view_alt", "workTableViewToggleBtn"),
    Filters(R.drawable.ic_dials, "casesVue.filters", "workIncidentFilterBtn"),
    Layers(R.drawable.ic_layers, "casesVue.layers", "workIncidentLayerBtn"),
    MapView(R.drawable.ic_map, "casesVue.map_view", "workMapViewToggleBtn"),
    ZoomToInteractive(
        R.drawable.ic_zoom_interactive,
        "worksiteMap.zoom_to_interactive",
        "workZoomToInteractiveBtn",
    ),
    ZoomToIncident(
        R.drawable.ic_zoom_incident,
        "worksiteMap.zoom_to_incident",
        "workZoomToIncidentBtn",
    ),
    ZoomIn(R.drawable.ic_plus, "actions.zoom_in", "workZoomInBtn"),
    ZoomOut(R.drawable.ic_minus, "actions.zoom_out", "workZoomOutBtn"),
}

@Composable
private fun CasesActionButton(
    modifier: Modifier = Modifier,
    action: CasesAction,
    onCasesAction: (CasesAction) -> Unit,
    shape: Shape = actionRoundCornerShape,
) {
    CrisisCleanupElevatedIconButton(
        modifier = modifier
            .actionSmallSize()
            .testTag(action.testTag),
        iconResId = action.iconResId,
        contentDescription = LocalAppTranslator.current(action.descriptionTranslateKey),
        onClick = { onCasesAction(action) },
        shape = shape,
    )
}

@Composable
fun CasesActionFlatButton(
    action: CasesAction,
    onCasesAction: (CasesAction) -> Unit,
    enabled: Boolean = false,
) {
    CrisisCleanupIconButton(
        modifier = Modifier.testTag(action.testTag),
        iconResId = action.iconResId,
        contentDescription = LocalAppTranslator.current(action.descriptionTranslateKey),
        onClick = { onCasesAction(action) },
        enabled = enabled,
    )
}

@Composable
internal fun CaseMapActionBar(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    filtersCount: Int = 0,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(adjacentButtonSpace),
    ) {
        CasesActionButton(
            modifier,
            CasesAction.Search,
            onCasesAction,
            shape = actionStartRoundCornerShape,
        )
        FilterButtonBadge(filtersCount) {
            CasesActionButton(
                modifier,
                CasesAction.Filters,
                onCasesAction,
                shape = actionEndRoundCornerShape,
            )
        }
    }
}

@Composable
internal fun CaseMapZoomBar(
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
        // CasesActionButton(modifier, CasesAction.Layers, onCasesAction)
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES, name = "Toggle to table")
@Composable
fun CaseMapActionBarPreview() {
    CaseMapActionBar()
}

@Preview
@Composable
fun CaseMapZoomBarPreview() {
    CaseMapZoomBar()
}
