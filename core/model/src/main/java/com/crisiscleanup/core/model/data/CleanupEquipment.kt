package com.crisiscleanup.core.model.data

enum class CleanupEquipment(val literal: String) {
    Unknown("unknown"),
    Chainsaw("equipment.chainsaw"),
    Van("equipment.van"),
    Bus("equipment.bus"),
    Pump("equipment.pump"),
    Compressor("equipment.compressor"),
    Trailer("equipment.trailer"),
    Backhoe("equipment.backhoe"),
    SkidSteer("equipment.skid_steer"),
    Bulldozer("equipment.bulldozer"),
    Excavator("equipment.excavator"),
    DumpTruck("equipment.dump_truck"),
    Forklift("equipment.forklift"),
}

private val literalEquipmentLookup = CleanupEquipment.entries.associateBy(CleanupEquipment::literal)
fun equipmentFromLiteral(literal: String) =
    literalEquipmentLookup[literal] ?: CleanupEquipment.Unknown

data class EquipmentData(
    val id: Long,
    val nameKey: String,
    val equipment: CleanupEquipment = equipmentFromLiteral(nameKey),
    val listOrder: Long?,
    val selectedCount: Int,
)
