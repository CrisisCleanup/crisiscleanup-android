package com.crisiscleanup.feature.cases.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupElevatedIconButton
import com.crisiscleanup.core.designsystem.component.actionBottomRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionEndRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionSmallSize
import com.crisiscleanup.core.designsystem.component.actionSmallSpace
import com.crisiscleanup.core.designsystem.component.actionStartRoundCornerShape
import com.crisiscleanup.core.designsystem.component.actionTopRoundCornerShape
import com.crisiscleanup.core.designsystem.component.adjacentButtonSpace
import com.crisiscleanup.feature.cases.R

enum class CasesAction(
    val iconResId: Int,
    val descriptionTranslateKey: String,
) {
    CreateNew(android.R.drawable.btn_plus, "nav.new_case"),
    Search(R.drawable.ic_search, "actions.search"),
    TableView(R.drawable.ic_table, "actions.table_view_alt"),
    Filters(R.drawable.ic_dials, "casesVue.filters"),
    Layers(R.drawable.ic_layers, "casesVue.layers"),
    MapView(R.drawable.ic_map, "casesVue.map_view"),
    ZoomToInteractive(R.drawable.ic_zoom_interactive, "worksiteMap.zoom_to_interactive"),
    ZoomToIncident(R.drawable.ic_zoom_incident, "worksiteMap.zoom_to_incident"),
    ZoomIn(R.drawable.ic_plus, "actions.zoom_in"),
    ZoomOut(R.drawable.ic_minus, "actions.zoom_out"),
}

@Composable
private fun CasesActionButton(
    modifier: Modifier = Modifier,
    action: CasesAction,
    onCasesAction: (CasesAction) -> Unit,
    shape: Shape = actionRoundCornerShape,
) {
    CrisisCleanupElevatedIconButton(
        modifier = modifier.actionSmallSize(),
        iconResId = action.iconResId,
        contentDescription = LocalAppTranslator.current.translator(action.descriptionTranslateKey),
        onClick = { onCasesAction(action) },
        shape = shape,
    )
}

@Composable
internal fun CasesActionBar(
    modifier: Modifier = Modifier,
    onCasesAction: (CasesAction) -> Unit = {},
    filtersCount: Int = 0,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(adjacentButtonSpace),
    ) {
        CasesActionButton(
            modifier, CasesAction.Search, onCasesAction, shape = actionStartRoundCornerShape
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
        // CasesActionButton(modifier, CasesAction.Layers, onCasesAction)
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