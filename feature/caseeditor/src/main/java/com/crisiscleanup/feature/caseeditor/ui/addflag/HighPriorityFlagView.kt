package com.crisiscleanup.feature.caseeditor.ui.addflag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextCheckbox
import com.crisiscleanup.core.designsystem.component.SmallBusyIndicator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listCheckboxAlignStartOffset
import com.crisiscleanup.core.designsystem.theme.listItemBottomPadding
import com.crisiscleanup.core.designsystem.theme.listItemHeight
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.primaryBlueColor
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.feature.caseeditor.CaseAddFlagViewModel
import com.crisiscleanup.feature.caseeditor.ui.PropertyInfoRow
import com.crisiscleanup.feature.caseeditor.ui.edgeSpacingHalf

@Composable
internal fun ColumnScope.HighPriorityView(
    viewModel: CaseAddFlagViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    isEditable: Boolean = false,
) {
    val translator = LocalAppTranslator.current.translator

    var isHighPriority by remember { mutableStateOf(false) }

    CrisisCleanupTextCheckbox(
        modifier = listItemModifier.listCheckboxAlignStartOffset(),
        checked = isHighPriority,
        onToggle = { isHighPriority = !isHighPriority },
        onCheckChange = { b: Boolean -> isHighPriority = b },
        text = translator("flag.flag_high_priority"),
        enabled = isEditable,
    )

    val organizations by viewModel.nearbyOrganizations.collectAsStateWithLifecycle()
    var flagNotes by remember { mutableStateOf("") }
    var selectedContacts by remember { mutableStateOf(emptyList<PersonContact>()) }

    LazyColumn(
        Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        item(contentType = "item-text") {
            Text(
                translator("flag.please_describe_why_high_priority"),
                Modifier.listItemHorizontalPadding(),
            )
        }

        item {
            TextArea(
                flagNotes,
                { text: String -> flagNotes = text },
                listItemModifier,
                isEditable = isEditable
            )
        }

        if (organizations == null) {
            item(contentType = "item-progress") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    SmallBusyIndicator()
                }
            }
        } else {
            item(contentType = "item-text") {
                Row(
                    Modifier
                        .listItemHorizontalPadding()
                        .listItemBottomPadding()
                        // TODO Common dimensions
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        translator("flag.nearby_organizations"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            textItem(
                translator("caseHistory.do_not_share_contact_warning"),
                isBold = true,
            )
            textItem(translator("caseHistory.do_not_share_contact_explanation"))

            items(
                organizations!!,
                key = { it.id },
                contentType = { "item-organization" },
            ) {
                Row(
                    Modifier
                        .clickable { selectedContacts = it.primaryContacts }
                        .then(listItemModifier.listItemHeight()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        it.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = primaryBlueColor,
                    )
                }
            }
        }
    }

    AddFlagSaveActionBar(
        onSave = { viewModel.onHighPriority(isHighPriority, flagNotes) },
        onCancel = onBack,
        isEditable = isEditable,
    )

    if (selectedContacts.isNotEmpty()) {
        ContactsDialog(
            selectedContacts,
        ) { selectedContacts = emptyList() }
    }
}

@Composable
private fun ContactsDialog(
    contacts: List<PersonContact>,
    onCloseDialog: () -> Unit,
) {
    val translator = LocalAppTranslator.current.translator
    CrisisCleanupAlertDialog(
        title = translator("flag.primary_contacts"),
        onDismissRequest = onCloseDialog,
        confirmButton = {
            CrisisCleanupTextButton(
                text = translator("actions.close"),
                onClick = onCloseDialog,
            )
        },
    ) {
        val spacingModifier = Modifier
            .padding(vertical = edgeSpacingHalf)
            .padding(bottom = edgeSpacingHalf)
        // TODO Common color
        LazyColumn(verticalArrangement = listItemSpacedBy) {
            items(
                contacts,
                key = { it.id },
                contentType = { "item-contact" },
            ) {
                with(it) {
                    Text(
                        "$firstName $lastName",
                        spacingModifier,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (email.isNotBlank()) {
                        PropertyInfoRow(
                            CrisisCleanupIcons.Mail,
                            email,
                            spacingModifier,
                            isEmail = true,
                        )
                    }
                    if (mobile.isNotBlank()) {
                        PropertyInfoRow(
                            CrisisCleanupIcons.Phone,
                            mobile,
                            spacingModifier,
                            isPhone = true,
                        )
                    }
                }
            }
        }
    }
}