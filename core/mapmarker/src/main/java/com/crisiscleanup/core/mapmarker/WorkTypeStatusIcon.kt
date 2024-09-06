package com.crisiscleanup.core.mapmarker

import com.crisiscleanup.core.model.data.WorkTypeType.AnimalServices
import com.crisiscleanup.core.model.data.WorkTypeType.Ash
import com.crisiscleanup.core.model.data.WorkTypeType.CatchmentGutters
import com.crisiscleanup.core.model.data.WorkTypeType.ChimneyCapping
import com.crisiscleanup.core.model.data.WorkTypeType.ConstructionConsultation
import com.crisiscleanup.core.model.data.WorkTypeType.CoreReliefItems
import com.crisiscleanup.core.model.data.WorkTypeType.Debris
import com.crisiscleanup.core.model.data.WorkTypeType.DeferredMaintenance
import com.crisiscleanup.core.model.data.WorkTypeType.Demolition
import com.crisiscleanup.core.model.data.WorkTypeType.DomesticServices
import com.crisiscleanup.core.model.data.WorkTypeType.Erosion
import com.crisiscleanup.core.model.data.WorkTypeType.Escort
import com.crisiscleanup.core.model.data.WorkTypeType.Favorite
import com.crisiscleanup.core.model.data.WorkTypeType.Fence
import com.crisiscleanup.core.model.data.WorkTypeType.Fire
import com.crisiscleanup.core.model.data.WorkTypeType.Food
import com.crisiscleanup.core.model.data.WorkTypeType.Important
import com.crisiscleanup.core.model.data.WorkTypeType.Landslide
import com.crisiscleanup.core.model.data.WorkTypeType.Leak
import com.crisiscleanup.core.model.data.WorkTypeType.Meals
import com.crisiscleanup.core.model.data.WorkTypeType.MoldRemediation
import com.crisiscleanup.core.model.data.WorkTypeType.MuckOut
import com.crisiscleanup.core.model.data.WorkTypeType.Other
import com.crisiscleanup.core.model.data.WorkTypeType.Oxygen
import com.crisiscleanup.core.model.data.WorkTypeType.Pipe
import com.crisiscleanup.core.model.data.WorkTypeType.Ppe
import com.crisiscleanup.core.model.data.WorkTypeType.Prescription
import com.crisiscleanup.core.model.data.WorkTypeType.Rebuild
import com.crisiscleanup.core.model.data.WorkTypeType.RebuildTotal
import com.crisiscleanup.core.model.data.WorkTypeType.RetardantCleanup
import com.crisiscleanup.core.model.data.WorkTypeType.Sandbagging
import com.crisiscleanup.core.model.data.WorkTypeType.Shelter
import com.crisiscleanup.core.model.data.WorkTypeType.Shopping
import com.crisiscleanup.core.model.data.WorkTypeType.SmokeDamage
import com.crisiscleanup.core.model.data.WorkTypeType.SnowGround
import com.crisiscleanup.core.model.data.WorkTypeType.SnowRoof
import com.crisiscleanup.core.model.data.WorkTypeType.Structure
import com.crisiscleanup.core.model.data.WorkTypeType.Tarp
import com.crisiscleanup.core.model.data.WorkTypeType.TemporaryHousing
import com.crisiscleanup.core.model.data.WorkTypeType.Trees
import com.crisiscleanup.core.model.data.WorkTypeType.TreesHeavyEquipment
import com.crisiscleanup.core.model.data.WorkTypeType.Unknown
import com.crisiscleanup.core.model.data.WorkTypeType.WaterBottles
import com.crisiscleanup.core.model.data.WorkTypeType.WellnessCheck

internal val statusIconLookup = mapOf(
    AnimalServices to R.drawable.ic_work_type_animal_services,
    Ash to R.drawable.ic_work_type_ash,
    CatchmentGutters to R.drawable.ic_work_type_catchment_gutters,
    ChimneyCapping to R.drawable.ic_work_type_chimney_capping,
    ConstructionConsultation to R.drawable.ic_work_type_construction_consultation,
    CoreReliefItems to R.drawable.ic_work_type_core_relief_items,
    Debris to R.drawable.ic_work_type_debris,
    DeferredMaintenance to R.drawable.ic_work_type_deferred_maintenance,
    Demolition to R.drawable.ic_work_type_demolition,
    DomesticServices to R.drawable.ic_work_type_domestic_services,
    Erosion to R.drawable.ic_work_type_erosion,
    Escort to R.drawable.ic_work_type_escort,
    Favorite to R.drawable.ic_work_type_favorite,
    Fence to R.drawable.ic_work_type_fence,
    Fire to R.drawable.ic_work_type_fire,
    Food to R.drawable.ic_work_type_food,
    Important to R.drawable.ic_work_type_important,
    Landslide to R.drawable.ic_work_type_landslide,
    Leak to R.drawable.ic_work_type_leak,
    Meals to R.drawable.ic_work_type_meals,
    MoldRemediation to R.drawable.ic_work_type_mold_remediation,
    MuckOut to R.drawable.ic_work_type_muck_out,
    Other to R.drawable.ic_work_type_other,
    Oxygen to R.drawable.ic_work_type_oxygen,
    Pipe to R.drawable.ic_work_type_pipe,
    Ppe to R.drawable.ic_work_type_ppe,
    Prescription to R.drawable.ic_work_type_prescription,
    Rebuild to R.drawable.ic_work_type_rebuild,
    RebuildTotal to R.drawable.ic_work_type_rebuild_total,
    RetardantCleanup to R.drawable.ic_work_type_retardant_cleanup,
    Sandbagging to R.drawable.ic_work_type_sandbagging,
    Shelter to R.drawable.ic_work_type_shelter,
    Shopping to R.drawable.ic_work_type_shopping,
    SmokeDamage to R.drawable.ic_work_type_smoke_damage,
    SnowGround to R.drawable.ic_work_type_snow_ground,
    SnowRoof to R.drawable.ic_work_type_snow_roof,
    Structure to R.drawable.ic_work_type_structure,
    Tarp to R.drawable.ic_work_type_tarp,
    TemporaryHousing to R.drawable.ic_work_type_temporary_housing,
    Trees to R.drawable.ic_work_type_trees,
    TreesHeavyEquipment to R.drawable.ic_work_type_trees_heavy_equipment,
    Unknown to R.drawable.ic_work_type_unknown,
    WaterBottles to R.drawable.ic_work_type_water_bottles,
    WellnessCheck to R.drawable.ic_work_type_wellness_check,
)
