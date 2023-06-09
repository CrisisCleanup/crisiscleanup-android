package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel
import com.crisiscleanup.feature.caseeditor.CaseFlagFlow
import com.crisiscleanup.feature.caseeditor.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseEditAddFlagRoute(
    onBack: () -> Unit = {},
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    val appTranslator = remember(viewModel) {
        AppTranslator(translator = translator)
    }

    var flagFlow by remember { mutableStateOf(CaseFlagFlow.None) }
    val updateFlagFlow = remember(viewModel) { { selected: CaseFlagFlow -> flagFlow = selected } }
    val flagFlowOptions by viewModel.flagFlows.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAppTranslator provides appTranslator,
    ) {
        Column {
            TopAppBarCancelAction(
                title = translator("caseForm.add_flag", R.string.add_flag),
                onAction = onBack,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .listItemHorizontalPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    // TODO This should be duplicated and use a different key
                    translator("events.object_flag"),
                    style = MaterialTheme.typography.bodyLarge,
                )
                FlagsDropdown(
                    flagFlow,
                    flagFlowOptions,
                    Modifier.listItemPadding(),
                    onSelectedFlagFlow = updateFlagFlow,
                )
            }
        }
    }
}

@Composable
private fun FlagsDropdown(
    selectedFlagFlow: CaseFlagFlow,
    options: Collection<CaseFlagFlow>,
    modifier: Modifier = Modifier,
    onSelectedFlagFlow: (CaseFlagFlow) -> Unit = {},
) {
    val translator = LocalAppTranslator.current.translator

    var contentWidth by remember { mutableStateOf(Size.Zero) }

    val selectedText = translator(selectedFlagFlow.translateKey)
    Box(modifier) {
        var showDropdown by remember { mutableStateOf(false) }
        BackHandler(showDropdown) {
            showDropdown = false
        }
        Row(
            Modifier
                .clickable(onClick = { showDropdown = true })
                .onGloballyPositioned {
                    contentWidth = it.size.toSize()
                }
                .then(listItemModifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selectedText,
                style = MaterialTheme.typography.bodyLarge,
            )

            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = translator("flag.choose_problem"),
            )
        }

        DropdownMenu(
            modifier = Modifier
                .width(with(LocalDensity.current) { contentWidth.width.toDp() }),
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            properties = PopupProperties(focusable = false)
        ) {
            for (option in options) {
                key(option.translateKey) {
                    DropdownMenuItem(
                        modifier = Modifier.optionItemHeight(),
                        text = { Text(translator(option.translateKey)) },
                        onClick = {
                            onSelectedFlagFlow(option)
                            showDropdown = false
                        },
                    )
                }
            }
        }
    }
}