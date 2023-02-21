package com.crisiscleanup.core.model.data

import kotlinx.datetime.Instant

data class WorkType(
    val id: Long,
    val createdAt: Instant? = null,
    val orgClaim: Long? = null,
    val nextRecurAt: Instant? = null,
    val phase: Int? = null,
    val recur: String? = null,
    val statusLiteral: String,
    val workTypeLiteral: String,
    val isClaimed: Boolean = orgClaim != null,
) {
    val status: WorkTypeStatus = statusFromLiteral(statusLiteral)
    val statusClaim = WorkTypeStatusClaim(status, isClaimed)
    val workType: WorkTypeType = typeFromLiteral(workTypeLiteral)
}

private fun statusFromLiteral(status: String) = when (status.lowercase()) {
    "open_unassigned" -> WorkTypeStatus.OpenUnassigned
    "open_assigned" -> WorkTypeStatus.OpenAssigned
    "open_partially-completed" -> WorkTypeStatus.OpenPartiallyCompleted
    "open_needs-follow-up" -> WorkTypeStatus.OpenNeedsFollowUp
    "open_unresponsive" -> WorkTypeStatus.OpenUnresponsive
    "closed_completed" -> WorkTypeStatus.ClosedCompleted
    "closed_incomplete" -> WorkTypeStatus.ClosedIncomplete
    "closed_out-of-scope" -> WorkTypeStatus.ClosedOutOfScope
    "closed_done-by-others" -> WorkTypeStatus.ClosedDoneByOthers
    else -> WorkTypeStatus.Unknown
}

enum class WorkTypeStatus {
    Unknown,
    OpenAssigned,
    OpenUnassigned,
    OpenPartiallyCompleted,
    OpenNeedsFollowUp,
    OpenUnresponsive,
    ClosedCompleted,
    ClosedIncomplete,
    ClosedOutOfScope,
    ClosedDoneByOthers,
}

data class WorkTypeStatusClaim(
    val status: WorkTypeStatus,
    val isClaimed: Boolean,
) {
    companion object {
        fun getType(type: String) = typeFromLiteral(type)

        fun make(status: String, orgId: Long?) = WorkTypeStatusClaim(
            statusFromLiteral(status),
            isClaimed = orgId != null,
        )
    }
}

private fun typeFromLiteral(type: String) = when (type.lowercase()) {
    "ash" -> WorkTypeType.Ash
    "animal_services" -> WorkTypeType.AnimalServices
    "catchment_gutters" -> WorkTypeType.CatchmentGutters
    "construction_consultation" -> WorkTypeType.ConstructionConsultation
    "core_relief_items" -> WorkTypeType.CoreReliefItems
    "demolition" -> WorkTypeType.Demolition
    "debris" -> WorkTypeType.Debris
    "deferred_maintenance" -> WorkTypeType.DeferredMaintenance
    "domestic_services" -> WorkTypeType.DomesticServices
    "erosion" -> WorkTypeType.Erosion
    "escort" -> WorkTypeType.Escort
    "fence" -> WorkTypeType.Fence
    "fire" -> WorkTypeType.Fire
    "food" -> WorkTypeType.Food
    "landslide" -> WorkTypeType.Landslide
    "leak" -> WorkTypeType.Leak
    "meals" -> WorkTypeType.Meals
    "mold_remediation" -> WorkTypeType.MoldRemediation
    "muck_out" -> WorkTypeType.MuckOut
    "other" -> WorkTypeType.Other
    "oxygen" -> WorkTypeType.Oxygen
    "pipe" -> WorkTypeType.Pipe
    "ppe" -> WorkTypeType.Ppe
    "prescription" -> WorkTypeType.Prescription
    "rebuild_total" -> WorkTypeType.RebuildTotal
    "rebuild" -> WorkTypeType.Rebuild
    "retardant_cleanup" -> WorkTypeType.RetardantCleanup
    "shelter" -> WorkTypeType.Shelter
    "shopping" -> WorkTypeType.Shopping
    "smoke_damage" -> WorkTypeType.SmokeDamage
    "snow_ground" -> WorkTypeType.SnowGround
    "snow_roof" -> WorkTypeType.SnowRoof
    "structure" -> WorkTypeType.Structure
    "tarp" -> WorkTypeType.Tarp
    "temporary_housing" -> WorkTypeType.TemporaryHousing
    "trees_heavy_equipment" -> WorkTypeType.TreesHeavyEquipment
    "trees" -> WorkTypeType.Trees
    "water_bottles" -> WorkTypeType.WaterBottles
    "wellness_check" -> WorkTypeType.WellnessCheck
    "sandbagging" -> WorkTypeType.Sandbagging
    "chimney_capping" -> WorkTypeType.ChimneyCapping
    else -> WorkTypeType.Unknown
}

enum class WorkTypeType {
    AnimalServices,
    Ash,
    CatchmentGutters,
    ChimneyCapping,
    ConstructionConsultation,
    CoreReliefItems,
    Debris,
    DeferredMaintenance,
    Demolition,
    DomesticServices,
    Erosion,
    Escort,
    Fence,
    Fire,
    Food,
    Landslide,
    Leak,
    Meals,
    MoldRemediation,
    MuckOut,
    Other,
    Oxygen,
    Pipe,
    Ppe,
    Prescription,
    Rebuild,
    RebuildTotal,
    RetardantCleanup,
    Sandbagging,
    Shelter,
    Shopping,
    SmokeDamage,
    SnowGround,
    SnowRoof,
    Structure,
    Tarp,
    TemporaryHousing,
    Trees,
    TreesHeavyEquipment,
    Unknown,
    WaterBottles,
    WellnessCheck,
}