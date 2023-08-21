package com.crisiscleanup.core.commoncase.ui

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
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.common.combineTrimText
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.designsystem.theme.listItemOptionPadding
import com.crisiscleanup.core.designsystem.theme.listRowItemStartPadding
import com.crisiscleanup.core.designsystem.theme.optionItemPadding

@Composable
private fun CaseView(
    caseSummary: CaseSummaryResult,
    modifier: Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        with(caseSummary) {
            icon?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = summary.workType?.workTypeLiteral,
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .listRowItemStartPadding(),
            ) {
                with(summary) {
                    Text(listOf(name, caseNumber).combineTrimText())
                    Text(listOf(address, city, state).combineTrimText())
                }
            }
        }
    }
}

fun LazyListScope.listCaseResults(
    worksites: List<CaseSummaryResult>,
    onCaseSelect: (CaseSummaryResult) -> Unit = {},
    itemKey: (CaseSummaryResult) -> Any = { it.listItemKey },
    isEditable: Boolean = false,
) {
    items(
        worksites,
        key = itemKey,
        contentType = { "item-worksite" },
    ) {
        CaseView(
            it,
            Modifier
                .testTag("workSearchResultItem_${it.listItemKey}")
                .clickable(
                    enabled = isEditable,
                    onClick = { onCaseSelect(it) },
                )
                .listItemOptionPadding(),
        )
    }
}

@Composable
fun ExistingCaseLocationsDropdownItems(
    worksites: List<CaseSummaryResult>,
    onCaseSelect: (CaseSummaryResult) -> Unit = {},
) {
    worksites.forEach { caseLocation ->
        DropdownMenuItem(
            text = {
                CaseView(
                    caseLocation,
                    Modifier
                        .fillMaxWidth()
                        .optionItemPadding(),
                )
            },
            onClick = { onCaseSelect(caseLocation) },
        )
    }
}
