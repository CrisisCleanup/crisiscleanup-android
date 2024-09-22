package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CollapsibleIcon
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.OutlinedClearableTextField
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.theme.LocalFontStyles
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.neutralFontColor
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.feature.team.MemberFilterResult

private fun LazyListScope.sectionHeaderItem(
    text: String,
    itemKey: String,
) {
    item(
        key = itemKey,
        contentType = "section-header",
    ) {
        Text(
            text,
            listItemModifier,
            style = LocalFontStyles.current.header3,
            color = neutralFontColor,
        )
    }
}

@Composable
internal fun EditTeamMembersView(
    members: List<PersonContact>,
    membersState: MemberFilterResult,
    onAddMember: (PersonContact) -> Unit,
    isEditable: Boolean,
    profilePictureLookup: Map<Long, String>,
    userRoleLookup: Map<Int, UserRole>,
    onToggleQrCode: () -> Unit = {},
    memberFilter: String = "",
    onUpdateMemberFilter: (String) -> Unit = {},
) {
    val t = LocalAppTranslator.current

    var isMemberDropdownExpanded by rememberSaveable { mutableStateOf(true) }

    // TODO Box layout with animated loading when filtering

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = listItemSpacedByHalf,
    ) {
        item {
            Box(
                Modifier
                    .clickable(onClick = onToggleQrCode)
                    .then(listItemModifier)
                    .actionHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    t("~~Show QR code to join"),
                    color = primaryBlueColor,
                    style = LocalFontStyles.current.header3,
                )
            }
        }

        item {
            Row(
                Modifier.clickable { isMemberDropdownExpanded = !isMemberDropdownExpanded }
                    .then(listItemModifier)
                    .actionHeight(),
                horizontalArrangement = listItemSpacedByHalf,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    t("~~Current team members ({member_count})")
                        .replace("{member_count}", "${members.size}"),
                    style = LocalFontStyles.current.header3,
                )

                Spacer(Modifier.weight(1f))

                val sectionTitle = t("~~currentTeamMembers")
                CollapsibleIcon(!isMemberDropdownExpanded, sectionTitle)
            }
        }

        if (isMemberDropdownExpanded) {
            items(
                members,
                key = { "member-${it.id}" },
                contentType = { "member-contact-item" },
            ) {
                TeamMemberContactCardView(
                    it,
                    profilePictureLookup,
                    userRoleLookup,
                    false,
                )
            }
        }

        sectionHeaderItem(
            t("~~Invite users to join"),
            itemKey = "invite-user-text",
        )

        sectionHeaderItem(
            t("~~Add user to team"),
            itemKey = "add-user-text",
        )

        item {
            OutlinedClearableTextField(
                modifier = listItemModifier.testTag("filterMembersTextField"),
                label = t("~~Filter users"),
                value = memberFilter,
                onValueChange = onUpdateMemberFilter,
                enabled = isEditable,
                isError = false,
                imeAction = ImeAction.Done,
            )
        }

        items(
            membersState.members,
            key = { "available-member-${it.person.id}" },
            contentType = { "available-member-item" },
        ) { personOrg ->
            val person = personOrg.person
            TeamMemberCardView(
                person,
                profilePictureLookup,
                userRoleLookup,
            ) {
                CrisisCleanupButton(
                    text = t("actions.add"),
                    onClick = { onAddMember(person) },
                    enabled = isEditable,
                )
            }
        }

        item {
            Spacer(Modifier.listItemBottomPadding())
        }
    }
}
