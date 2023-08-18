package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.WorkTypeAction
import com.crisiscleanup.core.designsystem.component.WorkTypePrimaryAction
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.feature.caseeditor.OrgClaimWorkType
import com.crisiscleanup.feature.caseeditor.WorkTypeSummary

@Composable
private fun ClaimingOrganization(
    name: String,
    isMyOrganization: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isMyOrganization) {
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(edgeSpacingHalf),
        ) {
            Text(name)
            val myOrganizationLabel =
                LocalAppTranslator.current("profileUser.your_organization")
            Text(
                "($myOrganizationLabel)",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    } else {
        Text(name, modifier)
    }
}

@Composable
private fun WorkTypeOrgClaims(
    isMyOrgClaim: Boolean,
    myOrgName: String,
    otherOrgClaims: List<String>,
    modifier: Modifier = Modifier,
) {
    Text(
        LocalAppTranslator.current("caseView.claimed_by"),
        modifier,
        style = MaterialTheme.typography.bodySmall,
    )
    Column(
        Modifier.padding(bottom = edgeSpacing),
        verticalArrangement = Arrangement.spacedBy(
            edgeSpacingHalf,
        ),
    ) {
        if (isMyOrgClaim) {
            ClaimingOrganization(myOrgName, true, modifier)
        }
        otherOrgClaims.forEach { otherOrganization ->
            ClaimingOrganization(otherOrganization, false, modifier)
        }
    }
}

@Composable
private fun WorkTypeSummaryView(
    summary: WorkTypeSummary,
    modifier: Modifier = Modifier,
    updateWorkType: (WorkType, Boolean) -> Unit = { _, _ -> },
    requestWorkType: (WorkType) -> Unit = {},
    releaseWorkType: (WorkType) -> Unit = {},
) = with(summary) {
    val updateWorkTypeStatus = { status: WorkTypeStatus ->
        updateWorkType(workType.copy(statusLiteral = status.literal), true)
    }
    CardSurface(elevation = 1.dp) {
        Column {
            Text(
                name,
                modifier.testTag("workTypeSummaryHeaderText").padding(top = edgeSpacing),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (jobSummary.isNotBlank()) {
                Text(
                    jobSummary,
                    modifier.testTag("workTypeSummarySubHeaderText").padding(top = edgeSpacingHalf),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = modifier.listItemVerticalPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkTypeStatusDropdown(workType.status, updateWorkTypeStatus)
                Spacer(Modifier.weight(1f))

                val t = LocalAppTranslator.current
                val isEditable = LocalCaseEditor.current.isEditable
                if (workType.isClaimed) {
                    if (isClaimedByMyOrg) {
                        WorkTypeAction(t("actions.unclaim"), isEditable) {
                            updateWorkType(workType.copy(orgClaim = null), false)
                        }
                    } else if (isReleasable) {
                        WorkTypeAction(t("actions.release"), isEditable) {
                            releaseWorkType(workType)
                        }
                    } else if (isRequested) {
                        Text(t("caseView.requested"))
                    } else {
                        WorkTypeAction(t("actions.request"), isEditable) {
                            requestWorkType(workType)
                        }
                    }
                } else {
                    WorkTypePrimaryAction(t("actions.claim"), isEditable) {
                        updateWorkType(workType.copy(orgClaim = myOrgId), false)
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.existingWorkTypeItems(
    sectionKey: String,
    summaries: List<WorkTypeSummary>,
    rowItemModifier: Modifier = Modifier,
    updateWorkType: (WorkType, Boolean) -> Unit = { _, _ -> },
    requestWorkType: (WorkType) -> Unit = {},
    releaseWorkType: (WorkType) -> Unit = {},
) {
    summaries.forEachIndexed { index, workTypeSummary ->
        val itemKey = "$sectionKey-$index"

        if (index > 0) {
            item(
                "section-work-type-summary-spacer-$itemKey",
                "content-work-type-summary-spacer-$itemKey",
            ) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        // TODO Common dimensions
                        .height(4.dp),
                )
            }
        }

        item(
            "section-work-type-summary-$itemKey",
            "content-work-type-summary-$itemKey",
        ) {
            WorkTypeSummaryView(
                workTypeSummary,
                rowItemModifier,
                updateWorkType,
                requestWorkType,
                releaseWorkType,
            )
        }
    }
}

internal fun LazyListScope.workTypeSectionTitle(
    titleTranslateKey: String,
    titleItemKey: String,
    modifier: Modifier = Modifier.listItemPadding(),
    isSmallTitle: Boolean = false,
) = item(
    "section-work-type-title-$titleItemKey",
    "section-work-type-title",
) {
    val style = if (isSmallTitle) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyLarge
    }
    Text(
        LocalAppTranslator.current(titleTranslateKey),
        modifier,
        style = style,
    )
}

internal fun LazyListScope.organizationWorkClaims(
    orgClaimWorkType: OrgClaimWorkType,
    rowItemModifier: Modifier = Modifier,
    updateWorkType: (WorkType, Boolean) -> Unit = { _, _ -> },
    requestWorkType: (WorkType) -> Unit = {},
    releaseWorkType: (WorkType) -> Unit = {},
) = with(orgClaimWorkType) {
    if (isMyOrg) {
        workTypeSectionTitle(
            "caseView.claimed_by_my_org",
            "claimed-by-$orgId",
        )
    } else {
        workTypeSectionTitle(
            "caseView.claimed_by",
            "claimed-by-$orgId-small",
            Modifier
                .listItemHorizontalPadding()
                .padding(top = 16.dp),
            isSmallTitle = true,
        )
        workTypeSectionTitle(
            orgName,
            "claimed-by-$orgId",
        )
    }

    existingWorkTypeItems(
        "org-$orgId",
        workTypes,
        rowItemModifier,
        updateWorkType,
        requestWorkType,
        releaseWorkType,
    )
}

@Preview
@Composable
private fun OrgClaimsPreview() {
    CrisisCleanupTheme {
        Surface {
            val otherOrgClaims = listOf(
                "Team green",
                "True blue",
                "Soarin Orange",
            )
            Column {
                WorkTypeOrgClaims(
                    true,
                    "My organization",
                    otherOrgClaims,
                )
            }
        }
    }
}
