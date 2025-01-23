package com.crisiscleanup.core.model.data

enum class CleanupEquipment(val literal: String) {
    Chainsaw("equipment.chainsaw"),
    Pump("equipment.pump"),
    Compressor("equipment.compressor"),
    Backhoe("equipment.backhoe"),
    Bulldozer("equipment.bulldozer"),
    Bus("equipment.bus"),
    DumpTruck("equipment.dump_truck"),
    Excavator("equipment.excavator"),
    SkidSteer("equipment.skid_steer"),
    Trailer("equipment.trailer"),
    Van("equipment.van"),
    Forklift("equipment.forklift"),
    Unknown("unknown"),
}

private val literalEquipmentLookup = CleanupEquipment.entries.associateBy(CleanupEquipment::literal)
fun equipmentFromLiteral(literal: String) =
    literalEquipmentLookup[literal] ?: CleanupEquipment.Unknown

data class EquipmentData(
    val id: Int,
    val nameKey: String,
    val equipment: CleanupEquipment = equipmentFromLiteral(nameKey),
    val listOrder: Int?,
    val selectedCount: Int,
    val quantity: Int,
)

val EmptyEquipment = EquipmentData(
    -1,
    "",
    listOrder = null,
    selectedCount = 0,
    quantity = 0,
)

data class MemberEquipment(
    val userId: Long,
    val userName: String,
    val equipmentData: EquipmentData,
)
