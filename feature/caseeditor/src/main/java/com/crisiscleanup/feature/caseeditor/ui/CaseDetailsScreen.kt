package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.EditCaseDetailsViewModel
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue
import org.apache.commons.text.StringEscapeUtils

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

    val breakGlassHint = remember(viewModel) { viewModel.translate("actions.edit") }
    val helpHint = remember(viewModel) { viewModel.translate("actions.help_alt") }

    var helpTitle by remember { mutableStateOf("") }
    var helpText by remember { mutableStateOf("") }
    val showHelp = remember(viewModel) {
        { data: FieldDynamicValue ->
            val text = data.field.help
            if (text.isNotBlank()) {
                helpTitle = data.field.label
                helpText = StringEscapeUtils.unescapeHtml4(text).toString()
            }
        }
    }

    val closeKeyboard = rememberCloseKeyboard(viewModel)
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .verticalScroll(scrollState)
            .fillMaxSize()
    ) {
        for (field in inputData.mutableFormFieldData) {
            var state by remember { field }
            // TODO Is it possible to isolate recomposition only to each changed item?
            //      key(){} is recomposing the entire list when only a single element changes.
            //      Try a simplified example first.
            key(state.key) {
//            // TODO There may be a label/placeholder on the field. Use by priority.
                val label = viewModel.translate(state.key)
                val fieldShowHelp = remember(viewModel) { { showHelp(state) } }
                DynamicFormListItem(
                    state,
                    label,
                    columnItemModifier,
                    breakGlassHint,
                    helpHint,
                    fieldShowHelp,
                ) { value: FieldDynamicValue ->
                    state = state.copy(
                        dynamicValue = value.dynamicValue,
                        breakGlass = value.breakGlass,
                    )
                }
            }
        }
    }

    if (helpText.isNotBlank()) {
        HelpDialog(
            title = helpTitle,
            text = helpText,
            onClose = { helpText = "" },
        )
    }
}

@Composable
private fun HelpDialog(
    title: String,
    text: String,
    onClose: () -> Unit = {},
) {
    AlertDialog(
        title = { Text(title) },
        text = { Text(text) },
        onDismissRequest = onClose,
        confirmButton = {
            CrisisCleanupTextButton(
                textResId = android.R.string.ok,
                onClick = onClose
            )
        },
    )
}

