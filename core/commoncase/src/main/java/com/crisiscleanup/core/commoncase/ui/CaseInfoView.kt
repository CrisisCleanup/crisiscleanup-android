package com.crisiscleanup.core.commoncase.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.commoncase.model.addressQuery
import com.crisiscleanup.core.designsystem.component.LinkifyEmailText
import com.crisiscleanup.core.designsystem.component.LinkifyLocationText
import com.crisiscleanup.core.designsystem.component.LinkifyPhoneText
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.model.data.Worksite

@Composable
fun PropertyInfoRow(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isPhone: Boolean = false,
    isEmail: Boolean = false,
    isLocation: Boolean = false,
    locationQuery: String = "",
    subText: String = "",
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = listItemSpacedBy,
    ) {
        Icon(
            imageVector = image,
            contentDescription = text,
            tint = neutralIconColor,
        )

        Column(
            Modifier.weight(1f),
            verticalArrangement = listItemSpacedByHalf,
        ) {
            val style = MaterialTheme.typography.bodyLarge
            if (isPhone) {
                LinkifyPhoneText(text)
            } else if (isEmail) {
                LinkifyEmailText(text)
            } else if (isLocation) {
                LinkifyLocationText(text, locationQuery)
            } else {
                Text(text, style = style)
            }

            if (subText.isNotBlank()) {
                Text(
                    subText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        trailingContent?.invoke()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CasePhoneInfoView(
    worksite: Worksite,
    enableCopy: Boolean,
    modifier: Modifier = Modifier,
    copyToClipboard: (String?) -> Unit = {},
) {
    val phoneNumbers =
        listOf(worksite.phone1, worksite.phone2).filterNotBlankTrim().joinToString("; ")
    val phoneNotes =
        listOf(worksite.phone1Notes, worksite.phone2Notes).filterNotBlankTrim()
            .joinToString("\n")
    val rowModifier = if (enableCopy) {
        Modifier
            .combinedClickable(
                onClick = {},
                onLongClick = { copyToClipboard(phoneNumbers) },
            )
            .then(modifier)
    } else {
        modifier
    }
    PropertyInfoRow(
        CrisisCleanupIcons.Phone,
        phoneNumbers,
        rowModifier,
        isPhone = true,
        subText = phoneNotes,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CaseAddressInfoView(
    worksite: Worksite,
    enableCopy: Boolean,
    modifier: Modifier = Modifier,
    copyToClipboard: (String?) -> Unit = {},
) {
    val (fullAddress, geoQuery, locationQuery) = worksite.addressQuery
    val rowModifier = if (enableCopy) {
        Modifier
            .combinedClickable(
                onClick = {},
                onLongClick = { copyToClipboard(fullAddress) },
            )
            .then(modifier)
    } else {
        modifier
    }
    PropertyInfoRow(
        CrisisCleanupIcons.Location,
        fullAddress,
        rowModifier.testTag("caseAddressInfo"),
        isLocation = true,
        locationQuery = geoQuery.ifBlank { locationQuery },
    ) {
        if (worksite.hasWrongLocationFlag) {
            ExplainWrongLocationDialog(worksite)
        }
    }
}
