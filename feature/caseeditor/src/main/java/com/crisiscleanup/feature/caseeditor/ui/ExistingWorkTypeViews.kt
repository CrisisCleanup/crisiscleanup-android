package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupOutlinedButton
import com.crisiscleanup.core.designsystem.theme.CrisisCleanupTheme
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.feature.caseeditor.OrgClaimWorkType
import com.crisiscleanup.feature.caseeditor.WorkTypeSummary

// Common colors
internal val cardContainerColor = Color.White

@Composable
internal fun CardSurface(
    containerColor: Color = cardContainerColor,
    cornerRound: Dp = 4.dp,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    CardSurface(
        Modifier
            .listItemHorizontalPadding()
            .fillMaxWidth(),
        containerColor,
        cornerRound,
        elevation,
        content,
    )
}

@Composable
internal fun CardSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = cardContainerColor,
    cornerRound: Dp = 4.dp,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRound),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = elevation,
    ) {
        content()
    }
}

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
            edgeSpacingHalf
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
    rowItemModifier: Modifier = Modifier,
    updateWorkType: (WorkType) -> Unit = {},
    requestWorkType: (WorkType) -> Unit = {},
    releaseWorkType: (WorkType) -> Unit = {},
) = with(summary) {
    val updateWorkTypeStatus = remember(updateWorkType) {
        { status: WorkTypeStatus ->
            updateWorkType(workType.copy(statusLiteral = status.literal))
        }
    }
    CardSurface(elevation = 1.dp) {
        Column {
            Text(
                name,
                rowItemModifier.padding(top = edgeSpacing),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (jobSummary.isNotBlank()) {
                Text(
                    jobSummary,
                    rowItemModifier.padding(top = edgeSpacingHalf),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = rowItemModifier.listItemVerticalPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkTypeStatusDropdown(workType.status, updateWorkTypeStatus)
                Spacer(Modifier.weight(1f))

                val translator = LocalAppTranslator.current
                if (workType.isClaimed) {
                    if (isClaimedByMyOrg) {
                        WorkTypeAction(translator("actions.unclaim")) {
                            updateWorkType(workType.copy(orgClaim = null))
                        }
                    } else if (isReleasable) {
                        WorkTypeAction(translator("actions.release")) { releaseWorkType(workType) }
                    } else if (isRequested) {
                        Text(translator("caseView.requested"))
                    } else {
                        WorkTypeAction(translator("actions.request")) { requestWorkType(workType) }
                    }
                } else {
                    WorkTypePrimaryAction(translator("actions.claim")) {
                        updateWorkType(workType.copy(orgClaim = myOrgId))
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkTypeAction(
    text: String,
    onClick: () -> Unit = {},
) = CrisisCleanupOutlinedButton(
    // TODO Common dimensions
    modifier = Modifier.widthIn(100.dp),
    text = text,
    onClick = onClick,
    enabled = LocalCaseEditor.current.isEditable,
)

@Composable
internal fun WorkTypePrimaryAction(
    text: String,
    onClick: () -> Unit = {},
) = CrisisCleanupButton(
    // TODO Common dimensions
    modifier = Modifier.widthIn(100.dp),
    text = text,
    onClick = onClick,
    enabled = LocalCaseEditor.current.isEditable,
    elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 1.dp,
    ),
)

internal fun LazyListScope.existingWorkTypeItems(
    sectionKey: String,
    summaries: List<WorkTypeSummary>,
    rowItemModifier: Modifier = Modifier,
    updateWorkType: (WorkType) -> Unit = {},
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
                        .height(4.dp)
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
    val style = if (isSmallTitle) MaterialTheme.typography.bodySmall
    else MaterialTheme.typography.bodyLarge
    Text(
        LocalAppTranslator.current(titleTranslateKey),
        modifier,
        style = style,
    )
}

internal fun LazyListScope.organizationWorkClaims(
    orgClaimWorkType: OrgClaimWorkType,
    rowItemModifier: Modifier = Modifier,
    updateWorkType: (WorkType) -> Unit = {},
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
            Modifier.listItemHorizontalPadding(),
            true,
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