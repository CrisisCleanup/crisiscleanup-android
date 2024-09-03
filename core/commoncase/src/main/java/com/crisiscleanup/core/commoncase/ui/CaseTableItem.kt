package com.crisiscleanup.core.commoncase.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.common.ParsedPhoneNumber
import com.crisiscleanup.core.commoncase.R
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.commoncase.oneDecimalFormat
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.WorksiteAddressButton
import com.crisiscleanup.core.designsystem.component.WorksiteAddressView
import com.crisiscleanup.core.designsystem.component.WorksiteCallButton
import com.crisiscleanup.core.designsystem.component.WorksiteNameView
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemCenterSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.model.data.Worksite

@Composable
private fun CaseFlagNumber(
    worksite: Worksite,
    distance: Double,
    isEditable: Boolean,
    onOpenFlags: () -> Unit,
) {
    val t = LocalAppTranslator.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-8).dp)
                // Similar to IconButton/IconButtonTokens.StateLayer*
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onOpenFlags,
                    enabled = isEditable,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val tint = LocalContentColor.current
            Icon(
                painterResource(R.drawable.ic_flag_filled_small),
                contentDescription = t("nav.flag"),
                tint = if (isEditable) tint else tint.disabledAlpha(),
                modifier = Modifier.testTag("tableViewItemFlagIcon"),
            )
        }
        Text(
            worksite.caseNumber,
            modifier = Modifier
                .testTag("tableViewItemCaseNumText")
                .offset(x = (-14).dp),
            style = LocalFontStyles.current.header3,
        )

        Spacer(modifier = Modifier.weight(1f))

        if (distance >= 0) {
            val distanceText = oneDecimalFormat.format(distance)
            Row {
                Text(
                    distanceText,
                    modifier = Modifier
                        .testTag("tableViewItemMilesAwayText")
                        .padding(end = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    t("caseView.miles_abbrv"),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("tableViewItemMilesAbbrev"),
                )
            }
        }
    }
}

@Composable
private fun WorksiteAssignTeamButton(
    isEditable: Boolean,
    onAssignToTeam: () -> Unit,
) {
    CrisisCleanupButton(
        onClick = onAssignToTeam,
        enabled = isEditable,
    ) {
        Icon(
            imageVector = CrisisCleanupIcons.AssignToTeam,
            contentDescription = LocalAppTranslator.current("~~Assign Case to team"),
            modifier = Modifier.testTag("assignCaseToTeamAction"),
        )
    }
}

val tableItemContentPadding = PaddingValues(
    // TODO Common dimensions
    horizontal = 16.dp,
    vertical = 8.dp,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseTableItem(
    worksite: Worksite,
    distance: Double,
    modifier: Modifier = Modifier,
    onViewCase: () -> Unit = {},
    onOpenFlags: () -> Unit = {},
    // TODO Add to team opens team select screen/dialog
    onAssignToTeam: () -> Unit = {},
    isEditable: Boolean = false,
    showPhoneNumbers: (List<ParsedPhoneNumber>) -> Unit = {},
    buttonContentPadding: PaddingValues = tableItemContentPadding,
    contentRowContent: @Composable () -> Unit = {},
    bottomRowTrailingContent: @Composable RowScope.() -> Unit = {},
) {
    val (fullAddress, geoQuery, locationQuery) = worksite.addressQuery

    Column(
        Modifier
            .clickable(
                onClick = onViewCase,
                enabled = isEditable,
            )
            .then(modifier),
        verticalArrangement = listItemSpacedBy,
    ) {
        CaseFlagNumber(
            worksite,
            distance,
            isEditable,
            onOpenFlags,
        )

        contentRowContent()

        WorksiteNameView(worksite.name)

        WorksiteAddressView(fullAddress) {
            if (worksite.hasWrongLocationFlag) {
                ExplainWrongLocationDialog(worksite)
            }
        }

        FlowRow(
            verticalArrangement = listItemCenterSpacedByHalf,
            horizontalArrangement = listItemSpacedBy,
        ) {
            WorksiteCallButton(
                phone1 = worksite.phone1,
                phone2 = worksite.phone2,
                enable = isEditable,
                contentPadding = buttonContentPadding,
                onShowPhoneNumbers = showPhoneNumbers,
            )

            WorksiteAddressButton(
                geoQuery = geoQuery,
                locationQuery = locationQuery,
                isEditable = isEditable,
                contentPadding = buttonContentPadding,
            )

            WorksiteAssignTeamButton(
                isEditable,
                onAssignToTeam,
            )

            bottomRowTrailingContent()
        }
    }
}
