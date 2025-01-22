package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.EquipmentEntity
import com.crisiscleanup.core.database.model.UserEquipmentEntity
import com.crisiscleanup.core.network.model.NetworkEquipment
import com.crisiscleanup.core.network.model.NetworkUserEquipment

fun NetworkEquipment.asEntity() = EquipmentEntity(
    id = id,
    listOrder = listOrder,
    isCommon = isCommon,
    selectedCount = selectedCount,
    nameKey = nameKey,
)

fun NetworkUserEquipment.asEntity() = UserEquipmentEntity(
    id = 0,
    localGlobalUuid = "",
    networkId = id,
    isLocalModified = false,
    userId = user,
    equipmentId = equipment,
    quantity = quantity,
)
