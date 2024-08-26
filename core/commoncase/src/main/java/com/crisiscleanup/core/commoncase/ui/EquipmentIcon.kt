package com.crisiscleanup.core.commoncase.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.commonassets.ui.EquipmentIcon
import com.crisiscleanup.core.commonassets.ui.getEquipmentIcon
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.model.data.CleanupEquipment

@Composable
fun EquipmentIcon(
    equipment: CleanupEquipment,
    modifier: Modifier = Modifier,
) {
    EquipmentIcon(
        getEquipmentIcon(equipment),
        LocalAppTranslator.current(equipment.literal),
        modifier,
    )
}
