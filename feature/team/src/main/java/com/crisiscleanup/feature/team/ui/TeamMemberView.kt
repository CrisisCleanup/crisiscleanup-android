package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.crisiscleanup.core.common.openDialer
import com.crisiscleanup.core.common.openEmail
import com.crisiscleanup.core.common.openSms
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AvatarIcon
import com.crisiscleanup.core.designsystem.component.CardSurface
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.LocalDimensions
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole

@Composable
internal fun TeamMemberCardView(
    person: PersonContact,
    profilePictureLookup: Map<Long, String>,
    userRoleLookup: Map<Int, UserRole>,
    trailingContent: @Composable (Size) -> Unit = {},
) {
    var contentSize by remember { mutableStateOf(Size.Zero) }
    CardSurface {
        Row(
            // TODO Common dimensions
            Modifier.padding(12.dp)
                .onGloballyPositioned {
                    contentSize = it.size.toSize()
                },
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = if (person.activeRoles.isEmpty()) {
                Alignment.CenterVertically
            } else {
                Alignment.Top
            },
        ) {
            Box(
                modifier = Modifier
                    .size(LocalDimensions.current.avatarCircleRadius)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AvatarIcon(
                    profilePictureLookup[person.id] ?: person.avatarUrl,
                    person.fullName,
                )
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = listItemSpacedByHalf,
            ) {
                Text(
                    person.fullName,
                    style = LocalFontStyles.current.header4,
                )
                for (userRoleCode in person.activeRoles) {
                    val role = userRoleLookup[userRoleCode]?.name ?: ""
                    if (role.isNotBlank()) {
                        Text(
                            role,
                            style = LocalFontStyles.current.helpTextStyle,
                        )
                    }
                }
            }

            trailingContent(contentSize)
        }
    }
}

@Composable
private fun TeamMemberOverflowMenu(
    contentSize: Size,
    onOpenEmail: () -> Unit,
) {
    val t = LocalAppTranslator.current
    var showDropdown by remember { mutableStateOf(false) }
    Box {
        CrisisCleanupIconButton(
            imageVector = CrisisCleanupIcons.MoreVert,
            onClick = { showDropdown = true },
            tint = neutralIconColor,
            contentDescription = t("actions.show_more"),
        )

        if (showDropdown) {
            DropdownMenu(
                modifier = Modifier
                    .sizeIn(maxWidth = 128.dp)
                    .width(
                        with(LocalDensity.current) {
                            contentSize.width.toDp()
                        },
                    ),
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                DropdownMenuItem(
                    modifier = Modifier.optionItemHeight(),
                    text = { Text(t("actions.email")) },
                    onClick = {
                        onOpenEmail()
                        showDropdown = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun TeamMemberContactCardView(
    person: PersonContact,
    profilePictureLookup: Map<Long, String>,
    userRoleLookup: Map<Int, UserRole>,
    showActions: Boolean,
    onActionError: (String) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    TeamMemberCardView(
        person,
        profilePictureLookup,
        userRoleLookup,
    ) { contentSize ->
        if (showActions) {
            val context = LocalContext.current
            val phoneNumber = person.mobile.trim()
            val hasPhone = phoneNumber.isNotBlank()
            if (hasPhone) {
                CrisisCleanupIconButton(
                    imageVector = CrisisCleanupIcons.Phone,
                    onClick = {
                        if (!context.openDialer(phoneNumber)) {
                            onActionError(t("~~This device does not support phone calls."))
                        }
                    },
                    tint = neutralIconColor,
                    contentDescription = t("actions.call"),
                )
                CrisisCleanupIconButton(
                    imageVector = CrisisCleanupIcons.Sms,
                    onClick = {
                        if (!context.openSms(phoneNumber)) {
                            onActionError(t("~~This device does not support text messaging."))
                        }
                    },
                    tint = neutralIconColor,
                    contentDescription = t("actions.chat"),
                )
            }
            val emailAddress = person.email.trim()
            if (emailAddress.isNotBlank()) {
                val openEmailOrError = remember(emailAddress) {
                    {
                        if (!context.openEmail(emailAddress)) {
                            onActionError(t("~~This device does not support sending emails."))
                        }
                    }
                }
                if (hasPhone) {
                    TeamMemberOverflowMenu(contentSize, openEmailOrError)
                } else {
                    CrisisCleanupIconButton(
                        imageVector = CrisisCleanupIcons.Email,
                        onClick = openEmailOrError,
                        tint = neutralIconColor,
                        contentDescription = t("actions.chat"),
                    )
                }
            }
        }
    }
}
