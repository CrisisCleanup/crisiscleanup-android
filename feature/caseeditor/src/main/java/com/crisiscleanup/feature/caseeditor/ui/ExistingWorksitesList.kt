package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.util.combineTrimText

@Composable
private fun CaseView(
    caseLocation: ExistingCaseLocation,
    modifier: Modifier,
) {
    Row(modifier) {
        with(caseLocation) {
            if (icon != null && workType != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = workType.workTypeLiteral,
                )
            }
            Column(
                androidx.compose.ui.Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(combineTrimText(name, caseNumber))
                Text(combineTrimText(address, city, state))
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
                .padding(16.dp),
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
                        // TODO Use common styles
                        .padding(vertical = 16.dp)
                )
            },
            onClick = { onCaseSelect(caseLocation) },
        )
    }
}