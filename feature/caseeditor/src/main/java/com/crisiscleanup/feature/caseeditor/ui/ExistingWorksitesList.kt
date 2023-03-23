package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.util.combineTrimText

internal fun LazyListScope.existingCaseLocations(
    worksites: List<ExistingCaseLocation>,
    onCaseSelect: (ExistingCaseLocation) -> Unit = {},
) {
    items(
        worksites,
        key = { it.networkWorksiteId },
        contentType = { "item-worksite" },
    ) { caseLocation ->
        Row(
            androidx.compose.ui.Modifier
                .clickable { onCaseSelect(caseLocation) }
                .padding(16.dp)) {
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
}