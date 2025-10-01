package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.ui.rememberCloseKeyboard
import com.crisiscleanup.core.ui.scrollFlingListener
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel
import com.crisiscleanup.feature.caseeditor.util.labelTextItem
import com.crisiscleanup.feature.caseeditor.util.listTextItem

@Composable
internal fun ColumnScope.WrongLocationFlagView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
    setWrongLocationFlag: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    val wrongLocationText by viewModel.wrongLocationText.collectAsStateWithLifecycle()
    val isProcessingLocation by viewModel.isProcessingLocation.collectAsStateWithLifecycle()
    val validCoordinates by viewModel.validCoordinates.collectAsStateWithLifecycle()
    val isValidCoordinates = validCoordinates != null

    val onWrongLocationTextChange =
        remember(viewModel) { { s: String -> viewModel.onWrongLocationTextChange(s) } }

    val closeKeyboard = rememberCloseKeyboard()

    val stepTranslateKeys = listOf(
        "flag.find_correct_google_maps",
        "flag.zoom_in_completely",
        "flag.copy_paste_url",
    )

    LazyColumn(
        Modifier
            .scrollFlingListener(closeKeyboard)
            .weight(1f)
            .fillMaxWidth(),
    ) {
        listTextItem(translator("flag.move_case_pin"))

        stepTranslateKeys.forEachIndexed { index, translateKey ->
            val number = index + 1
            labelTextItem("$number. ${translator(translateKey)}")
        }

        item {
            OutlinedClearableTextField(
                modifier = listItemModifier,
                labelResId = 0,
                label = translator("flag.google_map_url"),
                value = wrongLocationText,
                onValueChange = { onWrongLocationTextChange(it) },
                keyboardType = KeyboardType.Text,
                keyboardCapitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                isError = false,
                hasFocus = false,
                onEnter = closeKeyboard,
                enabled = isEditable,
            )
        }

        labelTextItem(translator("flag.click_if_location_unknown"))
        item {
            CrisisCleanupTextButton(
                listItemModifier,
                text = translator("flag.location_unknown"),
                onClick = setWrongLocationFlag,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                ),
            )
        }
    }

    val onSave = remember(viewModel, validCoordinates) {
        {
            viewModel.updateLocation(validCoordinates)
        }
    }
    AddFlagSaveActionBar(
        onSave = onSave,
        onCancel = onBack,
        enabled = isEditable,
        enableSave = isValidCoordinates,
        isBusy = isProcessingLocation,
    )
}
