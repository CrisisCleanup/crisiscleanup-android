package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.EquipmentEntity
import com.crisiscleanup.core.network.model.NetworkEquipment

fun NetworkEquipment.asEntity() = EquipmentEntity(
    id = id,
    listOrder = listOrder,
    isCommon = isCommon,
    selectedCount = selectedCount,
    nameKey = nameKey,
)
