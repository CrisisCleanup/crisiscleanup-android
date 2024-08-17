package com.crisiscleanup.core.database.model

import androidx.room.Embedded
import com.crisiscleanup.core.model.data.EquipmentData

data class PopulatedEquipment(
    @Embedded
    val entity: EquipmentEntity,
)

fun PopulatedEquipment.asExternalModel() = with(entity) {
    EquipmentData(
        id = id,
        nameKey = nameKey,
        listOrder = listOrder,
        selectedCount = selectedCount,
    )
}
