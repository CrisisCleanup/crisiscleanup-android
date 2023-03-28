package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.crisiscleanup.core.designsystem.theme.listItemOptionPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.optionItemPadding
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.util.combineTrimText

@Composable
private fun CaseView(
    caseLocation: ExistingCaseLocation,
    modifier: Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        with(caseLocation) {
            if (icon != null && workType != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = workType.workTypeLiteral,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .listRowItemStartPadding()
            ) {
                Text(listOf(name, caseNumber).combineTrimText())
                Text(listOf(address, city, state).combineTrimText())
            }
        }
    }
}

internal fun LazyListScope.existingCaseLocations(
    worksites: List<ExistingCaseLocation>,
    onCaseSelect: (ExistingCaseLocation) -> Unit = {},
) {
    items(
        worksites,
        key = { it.networkWorksiteId },
        contentType = { "item-worksite" },
    ) {
        CaseView(
            it,
            Modifier
                .clickable { onCaseSelect(it) }
                .listItemOptionPadding()
        )
    }
}

@Composable
internal fun ExistingCaseLocationsDropdownItems(
    worksites: List<ExistingCaseLocation>,
    onCaseSelect: (ExistingCaseLocation) -> Unit = {},
) {
    worksites.forEach { caseLocation ->
        DropdownMenuItem(
            text = {
                CaseView(
                    caseLocation,
                    Modifier
                        .fillMaxWidth()
                        .optionItemPadding()
                )
            },
            onClick = { onCaseSelect(caseLocation) },
        )
    }
}