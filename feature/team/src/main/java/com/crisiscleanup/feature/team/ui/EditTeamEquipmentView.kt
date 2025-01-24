package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.roundedOutline
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.core.designsystem.theme.optionItemHeight
import com.crisiscleanup.core.model.data.EmptyEquipment
import com.crisiscleanup.core.model.data.EmptyPersonContact
import com.crisiscleanup.core.model.data.EquipmentData
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.feature.team.SinglePersonEquipment
import kotlinx.coroutines.launch

@Composable
private fun AddEquipmentDropdown(
    text: String,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    dropdownOptions: @Composable (() -> Unit) -> Unit = {},
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box(
        Modifier
            .clickable(
                enabled = enabled,
                onClick = { showDropdown = true },
            )
            .roundedOutline()
            .listItemPadding()
            .then(modifier),
    ) {
        Row(
            Modifier.listItemVerticalPadding(),
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                Modifier.weight(1f),
            )

            Icon(
                imageVector = CrisisCleanupIcons.ArrowDropDown,
                contentDescription = contentDescription,
            )
        }

        val closeDropdown = { showDropdown = false }
        DropdownMenu(
            showDropdown,
            closeDropdown,
        ) {
            dropdownOptions(closeDropdown)
        }
    }
}

@Composable
internal fun EditTeamEquipmentView(
    equipmentOptions: List<EquipmentData>,
    memberOptions: List<PersonContact>,
    teamEquipment: List<SinglePersonEquipment>,
    enableAddEquipment: Boolean = true,
    onAddEquipment: suspend (Long, Int, Int) -> Boolean = { _, _, _ -> false },
    onRemoveEquipment: (Long, Int) -> Unit = { _, _ -> },
) {
    val t = LocalAppTranslator.current

    var selectedEquipment by remember { mutableStateOf(EmptyEquipment) }
    var selectedTeamMember by remember { mutableStateOf(EmptyPersonContact) }
    var equipmentQuantityNormalized by remember { mutableFloatStateOf(0f) }
    val equipmentQuantity = Math.round(equipmentQuantityNormalized * 19) + 1

    var isAddingEquipment by remember { mutableStateOf(false) }
    val isAddEditable = enableAddEquipment && !isAddingEquipment

    val selectedEquipmentText = if (selectedEquipment == EmptyEquipment) {
        t("~~Equipment")
    } else {
        t(selectedEquipment.nameKey)
    }

    val selectedTeamMemberText = if (selectedTeamMember == EmptyPersonContact) {
        t("~~Owner")
    } else {
        selectedTeamMember.fullName
    }

    val coroutineScope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "add-equipment-form") {
            Column(
                listItemModifier,
                horizontalAlignment = Alignment.End,
                verticalArrangement = listItemSpacedBy,
            ) {
                AddEquipmentDropdown(
                    selectedEquipmentText,
                    contentDescription = t("~~Select equipment"),
                    enabled = isAddEditable,
                    Modifier.fillMaxWidth(),
                ) { closeOptions: () -> Unit ->
                    for (option in equipmentOptions) {
                        DropdownMenuItem(
                            modifier = Modifier.optionItemHeight(),
                            text = {
                                Text(t(option.nameKey))
                            },
                            onClick = {
                                selectedEquipment = option
                                closeOptions()
                            },
                        )
                    }
                }

                AddEquipmentDropdown(
                    selectedTeamMemberText,
                    contentDescription = t("~~Select team member"),
                    enabled = isAddEditable,
                    Modifier.fillMaxWidth(),
                ) { closeOptions: () -> Unit ->
                    for (option in memberOptions) {
                        DropdownMenuItem(
                            modifier = Modifier.optionItemHeight(),
                            text = {
                                Text(option.fullName)
                            },
                            onClick = {
                                selectedTeamMember = option
                                closeOptions()
                            },
                        )
                    }
                }

                Row(
                    horizontalArrangement = listItemSpacedByHalf,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        equipmentQuantityNormalized,
                        { equipmentQuantityNormalized = it },
                        Modifier.weight(1f),
                        enabled = isAddEditable,
                        // TODO Change colors to match theme
                    )

                    Box(
                        // TODO Common dimensions
                        Modifier.sizeIn(minWidth = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            equipmentQuantity.toString(),
                        )
                    }
                }

                CrisisCleanupButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isAddingEquipment = true
                                val isAdded = onAddEquipment(
                                    selectedTeamMember.id,
                                    selectedEquipment.id,
                                    equipmentQuantity,
                                )
                                if (isAdded) {
                                    // TODO Clear
                                } else {
                                    // TODO Show error
                                }
                            } finally {
                                isAddingEquipment = false
                            }
                        }
                    },
                    enabled = isAddEditable,
                    text = t("actions.add"),
                )
            }
        }

        for (personEquipments in teamEquipment) {
            if (personEquipments.equipment.isNotEmpty()) {
                val person = personEquipments.person
                item(
                    key = "person-${person.id}",
                    contentType = "item-person",
                ) {
                    Text(
                        person.fullName,
                        listItemModifier,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }

                items(
                    personEquipments.equipment,
                    key = { "equipment-${person.id}-${it.id}" },
                    contentType = { "item-equipment" },
                ) {
                    Row(
                        listItemModifier,
                        horizontalArrangement = listItemSpacedByHalf,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            t(it.nameKey),
                        )

                        if (it.quantity > 1) {
                            Text("(${it.quantity})")
                        }

                        Spacer(Modifier.weight(1f))

                        CrisisCleanupIconButton(
                            imageVector = CrisisCleanupIcons.Delete,
                            contentDescription = t("~~Remove {equipment_name} from {person_name}")
                                .replace("{equipment_name}", t(it.nameKey))
                                .replace("{person_name}", person.fullName),
                            onClick = {
                                // TODO
                            },
                            // TODO Enable/disable accordingly
                            enabled = true,
                        )
                    }
                }
            }
        }
    }
}
