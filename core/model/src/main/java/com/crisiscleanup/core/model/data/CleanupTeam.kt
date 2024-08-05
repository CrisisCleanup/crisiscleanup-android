package com.crisiscleanup.core.model.data

data class CleanupTeam(
    val id: Long,
    val name: String,
    val notes: String,
    val caseCount: Int,
    private val caseCompleteCount: Int,
    val caseCompletePercentage: Int =
        if (caseCount > 0) {
            (caseCompleteCount.toFloat() / caseCount * 100).toInt()
        } else {
            0
        },
    val incidentId: Long,
    val memberIds: List<Long>,
    val members: List<PersonContact>,
    val equipment: List<CleanupEquipment>,
)

data class PersonEquipment(
    val userId: Long,
    val equipment: CleanupEquipment,
    val count: Int = 1,
)

enum class CleanupEquipment(val literal: String) {
    Unknown("unknown"),
    Chainsaw("chainsaw"),
    Van("van"),
    Bus("bus"),
    Pump("pump"),
    Compressor("compressor"),
    Trailer("trailer"),
    Backhoe("backhoe"),
    SkidSteer("skid_steer"),
    Bulldozer("bulldozer"),
    Excavator("excavator"),
    DumpTruck("dump_truck"),
    Forklift("forklift"),
}

private val literalEquipmentLookup = CleanupEquipment.entries.associateBy(CleanupEquipment::literal)
fun equipmentFromLiteral(literal: String) =
    literalEquipmentLookup[literal] ?: CleanupEquipment.Unknown
