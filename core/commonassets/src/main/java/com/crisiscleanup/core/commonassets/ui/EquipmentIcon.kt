package com.crisiscleanup.core.commonassets.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.commonassets.R
import com.crisiscleanup.core.designsystem.theme.neutralIconColor
import com.crisiscleanup.core.designsystem.theme.separatorColor
import com.crisiscleanup.core.model.data.CleanupEquipment

private val equipmentIcons = mapOf(
    CleanupEquipment.Unknown to R.drawable.ic_equipment_other,
    CleanupEquipment.Chainsaw to R.drawable.ic_equipment_chainsaw,
//    CleanupEquipment.Van to R.drawable.ic_equipment_van,
//    CleanupEquipment.Bus to R.drawable.ic_equipment_bus,
//    CleanupEquipment.Pump to R.drawable.ic_equipment_pump,
//    CleanupEquipment.Compressor to R.drawable.ic_equipment_compressor,
    CleanupEquipment.Trailer to R.drawable.ic_equipment_trailer,
    CleanupEquipment.Backhoe to R.drawable.ic_equipment_backhoe,
//    CleanupEquipment.SkidSteer to R.drawable.ic_equipment_skid_steer,
    CleanupEquipment.Bulldozer to R.drawable.ic_equipment_bobcat,
//    CleanupEquipment.Excavator to R.drawable.ic_equipment_excavator,
//    CleanupEquipment.DumpTruck to R.drawable.ic_equipment_dump_truck,
//    CleanupEquipment.Forklift to R.drawable.ic_equipment_forklift,
)

fun getEquipmentIcon(equipment: CleanupEquipment) =
    equipmentIcons[equipment] ?: R.drawable.ic_equipment_other

@Composable
fun EquipmentIcon(
    @DrawableRes equipmentResId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = separatorColor,
        contentColor = neutralIconColor,
    ) {
        Icon(
            modifier = Modifier.padding(2.dp),
            painter = painterResource(equipmentResId),
            contentDescription = contentDescription,
        )
    }
}
