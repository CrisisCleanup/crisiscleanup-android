package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCaseDetailsViewModel
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue

private const val ScreenTitleTranslateKey = "property_info"

@Composable
internal fun DetailsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
    translate: (String) -> String = { s -> s },
) {
    EditCaseSummaryHeader(
        0,
        isEditable,
        onEdit,
        modifier,
        header = translate(ScreenTitleTranslateKey),
    ) {
        worksite.run {
            // TODO
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseDetailsRoute(
    viewModel: EditCaseDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    BackHandler {
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

    val onNavigateBack = remember(viewModel) {
        {
            if (viewModel.onNavigateBack()) {
                onBackClick()
            }
        }
    }
    val onNavigateCancel = remember(viewModel) {
        {
            if (viewModel.onNavigateCancel()) {
                onBackClick()
            }
        }
    }
    Column {
        // TODO This seems to be recomposing when map is moved.
        //      Is it possible to not recompose due to surrounding views?
        TopAppBarBackCancel(
            title = viewModel.translate(ScreenTitleTranslateKey),
            onBack = onNavigateBack,
            onCancel = onNavigateCancel,
        )
        // TODO Remove the gap between the top bar above and location view below
        DetailsView()
    }
}

@Composable
private fun DetailsView(
    viewModel: EditCaseDetailsViewModel = hiltViewModel(),
) {
    val inputData = viewModel.detailsInputData
    val fieldMap = inputData.fieldMap
    // TODO How to contain mutations to affected fields/items and not recompose entire (visible) list on changes? If it is possible in a maintainable way.
    val fieldValues = remember { inputData.snapshotMap }

    val breakGlassHint = remember(viewModel) { { viewModel.translate("actions.edit") } }

    val closeKeyboard = rememberCloseKeyboard(viewModel)
    val lazyListState = rememberLazyListState()
    LazyColumn(
        Modifier.scrollFlingListener(closeKeyboard),
        state = lazyListState,
    ) {
        items(
            inputData.formKeys,
            key = { it },
            contentType = { fieldMap[it]!!.listItemContentType },
        ) {
            // TODO There may be a label/placeholder on the field. Use by priority.
            val label = viewModel.translate(it)
            val fieldState = fieldMap[it]!!
            DynamicFormListItem(
                it,
                label,
                fieldState.field.htmlType,
                fieldValues[it]!!,
                columnItemModifier,
                breakGlassHint(),
                fieldState.field.help,
                // TODO Alert dialog show help
                {},
            ) { value: FieldDynamicValue ->
                fieldValues[it] = value
            }
        }
    }
}
