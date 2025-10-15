package com.crisiscleanup.feature.caseeditor.util

import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.IncidentFormField
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorkTypeStatus
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.feature.caseeditor.model.FieldDynamicValue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class WorkTypeUtilTest {

    // TODO: Frequency work types

    private val startingWorkTypesA: Worksite
        get() = testWorksite(
            workTypes = listOf(
                testWorkType("smoke_damage"),
                testWorkType("deferred_maintenance"),
            ),
        )

    private val workTypeLookup = mapOf(
        "tarps_needed" to "tarp",
        "mold_scraping" to "mold_remediation",
        "mold_remediation_info" to "mold_remediation",
        "floors_affected" to "muck_out",
        "mold_hvac" to "mold_remediation",
        "house_roof_damage" to "tarp",
        "needs_visual" to "rebuild",
        "roof_type" to "tarp",
        "roof_pitch" to "tarp",
        "outbuilding_roof_damage" to "tarp",
        "flood_height_select" to "muck_out",
        "tile_removal" to "muck_out",
        "appliance_removal" to "muck_out",
        "debris_description" to "debris",
        "mold_drying" to "mold_remediation",
        "mold_replace_studs" to "mold_remediation",
        "carpet_removal" to "muck_out",
        "nonvegitative_debris_removal" to "debris",
        "vegitative_debris_removal" to "debris",
        "mold_amount" to "mold_remediation",
        "ceiling_water_damage" to "muck_out",
        "rebuild_details" to "rebuild",
        "heavy_machinary_required" to "debris",
        "rebuild_info" to "rebuild",
        "notes" to "mold_remediation",
        "drywall_removal" to "muck_out",
        "help_install_tarp" to "tarp",
        "muck_out_info" to "muck_out",
        "tarping_info" to "tarp",
        "unsalvageable_structure" to "debris",
        "debris_info" to "debris",
        "mold_spraying" to "mold_remediation",
        "heavy_item_removal" to "muck_out",
        "tree_info" to "trees",
        "num_wide_trees" to "trees",
        "num_trees_down" to "trees",
        "interior_debris_removal" to "debris",
        "hardwood_floor_removal" to "muck_out",
    )

    private val formFieldLookup = mapOf(
        "needs_visual" to testFormField("workInfo", "needs_visual", "rebuild_info", "rebuild"),
        "claim_status_report_info" to testFormField("caseInfo", "claim_status_report_info", ""),
        "nonvegitative_debris_removal" to testFormField(
            "workInfo",
            "nonvegitative_debris_removal",
            "debris_info",
            "debris",
        ),
        "ceiling_water_damage" to testFormField(
            "workInfo",
            "ceiling_water_damage",
            "muck_out_info",
            "muck_out",
        ),
        "vegitative_debris_removal" to testFormField(
            "workInfo",
            "vegitative_debris_removal",
            "debris_info",
            "debris",
        ),
        "debris_description" to testFormField(
            "workInfo",
            "debris_description",
            "debris_info",
            "debris",
        ),
        "heavy_item_removal" to testFormField(
            "workInfo",
            "heavy_item_removal",
            "muck_out_info",
            "muck_out",
        ),
        "drywall_removal" to testFormField(
            "workInfo",
            "drywall_removal",
            "muck_out_info",
            "muck_out",
        ),
        "power_status" to testFormField("workInfo", "power_status", "utilities_info"),
        "tarping_info" to testFormField("workInfo", "tarping_info", "work_info", "tarp"),
        "habitable" to testFormField("caseInfo", "habitable", "hazards_info"),
        "mold_spraying" to testFormField(
            "workInfo",
            "mold_spraying",
            "mold_remediation_info",
            "mold_remediation",
        ),
        "house_roof_damage" to testFormField(
            "workInfo",
            "house_roof_damage",
            "tarping_info",
            "tarp",
        ),
        "carpet_removal" to testFormField(
            "workInfo",
            "carpet_removal",
            "muck_out_info",
            "muck_out",
        ),
        "residence_type" to testFormField("caseInfo", "residence_type", "property_info"),
        "mold_amount" to testFormField(
            "workInfo",
            "mold_amount",
            "mold_remediation_info",
            "mold_remediation",
        ),
        "num_trees_down" to testFormField("workInfo", "num_trees_down", "tree_info", "trees"),
        "gas_status" to testFormField("workInfo", "gas_status", "utilities_info"),
        "work_info" to testFormField("workInfo", "work_info", ""),
        "heavy_machinary_required" to testFormField(
            "workInfo",
            "heavy_machinary_required",
            "debris_info",
            "debris",
        ),
        "interior_debris_removal" to testFormField(
            "workInfo",
            "interior_debris_removal",
            "debris_info",
            "debris",
        ),
        "veteran" to testFormField("personalInfo", "veteran", "property_info"),
        "tarps_needed" to testFormField("workInfo", "tarps_needed", "tarping_info", "tarp"),
        "tree_info" to testFormField("workInfo", "tree_info", "work_info", "trees"),
        "debris_info" to testFormField("workInfo", "debris_info", "work_info", "debris"),
        "mold_remediation_info" to testFormField(
            "workInfo",
            "mold_remediation_info",
            "work_info",
            "mold_remediation",
        ),
        "mold_hvac" to testFormField(
            "workInfo",
            "mold_hvac",
            "mold_remediation_info",
            "mold_remediation",
        ),
        "water_status" to testFormField("caseInfo", "water_status", "utilities_info"),
        "hazards_info" to testFormField("caseInfo", "hazards_info", ""),
        "rebuild_info" to testFormField("workInfo", "rebuild_info", "work_info", "rebuild"),
        "debris_status" to testFormField("caseInfo", "debris_status", "claim_status_report_info"),
        "help_install_tarp" to testFormField(
            "workInfo",
            "help_install_tarp",
            "tarping_info",
            "tarp",
        ),
        "hardwood_floor_removal" to testFormField(
            "workInfo",
            "hardwood_floor_removal",
            "muck_out_info",
            "muck_out",
        ),
        "num_stories" to testFormField("workInfo", "num_stories", "tarping_info"),
        "muck_out_info" to testFormField("workInfo", "muck_out_info", "work_info", "muck_out"),
        "num_wide_trees" to testFormField("workInfo", "num_wide_trees", "tree_info", "trees"),
        "unsalvageable_structure" to testFormField(
            "workInfo",
            "unsalvageable_structure",
            "debris_info",
            "debris",
        ),
        "tarp_info" to testFormField("workInfo", "tarp_info", "work_info", "tarp"),
        "property_info" to testFormField("personalInfo", "property_info", ""),
        "rebuild_details" to testFormField(
            "workInfo",
            "rebuild_details",
            "rebuild_info",
            "rebuild",
        ),
    )

    private fun makeFormFieldData(
        formData: Map<String, WorksiteFormValue>,
        statusLookup: Map<String, WorkTypeStatus> = emptyMap(),
    ) = formData
        .map {
            mutableStateOf(
                FieldDynamicValue(
                    field = IncidentFormField(
                        label = "",
                        // TODO Set when frequency tests are written
                        htmlType = "",
                        group = "",
                        help = "",
                        placeholder = "",
                        validation = "",
                        valuesDefault = null,
                        values = emptyMap(),
                        isCheckboxDefaultTrue = false,
                        recurDefault = "",
                        isRequired = false,
                        isReadOnly = false,
                        isReadOnlyBreakGlass = false,
                        labelOrder = 0,
                        listOrder = 0,
                        isInvalidated = false,
                        fieldKey = it.key,
                        parentKey = formFieldLookup[it.key]?.parentKey ?: "",
                        selectToggleWorkType = "",
                    ),
                    selectOptions = emptyMap(),
                    dynamicValue = DynamicValue(
                        it.value.valueString,
                        isBoolean = it.value.isBoolean,
                        it.value.valueBoolean,
                    ),
                    workTypeStatus = statusLookup[workTypeLookup[it.key] ?: "notaworktype"]
                        ?: WorkTypeStatus.OpenUnassigned,
                ),
            )
        }

    // Test variations for work type (with status change where applicable)
    // - Empty work types
    //   - No change
    //   - Add
    // - Existing work types
    //   - No change
    //   - Add
    //   - Remove

    @Test
    fun newWorksiteNoWorkTypeFormData() {
        val worksite = testWorksite()
        val formFieldData = makeFormFieldData(
            mapOf(
                "property_info" to WorksiteFormValue(true, ""),
                "habitable" to WorksiteFormValue.trueValue,
                "residence_type" to WorksiteFormValue(valueString = "residence-type"),
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        assertEquals(
            worksite,
            updatedWorksite,
        )
    }

    @Test
    fun newWorksiteNewWorkTypeFormData() {
        val worksite = testWorksite()
        val formFieldData = makeFormFieldData(
            mapOf(
                "mold_remediation_info" to WorksiteFormValue(true, ""),
                "tree_info" to WorksiteFormValue.trueValue,
                "num_trees_down" to WorksiteFormValue(valueString = "3"),
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "num_wide_trees" to WorksiteFormValue(valueString = "5"),
                "debris_info" to WorksiteFormValue.trueValue,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )

        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("debris"),
                testWorkType("trees"),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun newWorksiteNewWorkTypeFormDataDifferentStatus() {
        val worksite = testWorksite()
        val formFieldData = makeFormFieldData(
            mapOf(
                "mold_remediation_info" to WorksiteFormValue(true, ""),
                "tree_info" to WorksiteFormValue.trueValue,
                "num_trees_down" to WorksiteFormValue(valueString = "3"),
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "num_wide_trees" to WorksiteFormValue(valueString = "5"),
                "debris_info" to WorksiteFormValue.trueValue,
            ),
            mapOf("trees" to WorkTypeStatus.ClosedDoneByOthers),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )

        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("debris"),
                testWorkType("trees", WorkTypeStatus.ClosedDoneByOthers),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun existingWorksiteDeleteWorkType() {
        val worksite = testWorksite().copy(
            workTypes = listOf(
                testWorkType(
                    "construction_consultation",
                    WorkTypeStatus.ClosedNoHelpWanted,
                ),
            ),
        )
        val formFieldData = makeFormFieldData(
            mapOf(
                "mold_remediation_info" to WorksiteFormValue(true, ""),
                "tree_info" to WorksiteFormValue.trueValue,
                "num_trees_down" to WorksiteFormValue(valueString = "3"),
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "num_wide_trees" to WorksiteFormValue(valueString = "5"),
                "debris_info" to WorksiteFormValue.trueValue,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("debris"),
                testWorkType("trees"),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun existingWorksiteNoWorkTypeChange() {
        val worksite = testWorksite()
            .copy(
                workTypes = listOf(
                    testWorkType("tarp", WorkTypeStatus.OpenAssigned),
                ),
            )
        val formFieldData = makeFormFieldData(
            mapOf(
                // Wouldn't be possible in app. For testing behavior.
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "tarping_info" to WorksiteFormValue.trueValue,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("tarp"),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun existingWorksiteNewWorkTypeFormData() {
        val worksite = testWorksite().copy(
            workTypes = listOf(
                testWorkType("tarp", WorkTypeStatus.OpenAssigned),
            ),
        )
        val formFieldData = makeFormFieldData(
            mapOf(
                "tree_info" to WorksiteFormValue.trueValue,
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "tarping_info" to WorksiteFormValue.trueValue,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("tarp"),
                testWorkType("trees"),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun existingWorksiteDeleteWorkTypeDifferentStatus() {
        val worksite = testWorksite().copy(
            workTypes = listOf(
                testWorkType("construction_consultation", WorkTypeStatus.ClosedNoHelpWanted),
                testWorkType("debris"),
            ),
        )
        val formFieldData = makeFormFieldData(
            mapOf(
                "mold_remediation_info" to WorksiteFormValue(true, ""),
                "tree_info" to WorksiteFormValue.trueValue,
                "num_trees_down" to WorksiteFormValue(valueString = "3"),
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "num_wide_trees" to WorksiteFormValue(valueString = "5"),
                "debris_info" to WorksiteFormValue.trueValue,
            ),
            mapOf(
                "construction_consultation" to WorkTypeStatus.ClosedDoneByOthers,
                "debris" to WorkTypeStatus.OpenPartiallyCompleted,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("debris", WorkTypeStatus.OpenPartiallyCompleted),
                testWorkType("trees"),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun existingWorksiteNoWorkTypeChangeDifferentStatus() {
        val worksite = testWorksite()
            .copy(
                workTypes = listOf(
                    testWorkType("tarp", WorkTypeStatus.OpenAssigned),
                    testWorkType("debris", WorkTypeStatus.ClosedOutOfScope),
                ),
            )
        val formFieldData = makeFormFieldData(
            mapOf(
                // Wouldn't be possible in app. For testing behavior.
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "debris_info" to WorksiteFormValue.trueValue,
                "tarping_info" to WorksiteFormValue.trueValue,
            ),
            mapOf(
                "tarp" to WorkTypeStatus.ClosedRejected,
                "debris" to WorkTypeStatus.OpenNeedsFollowUp,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("tarp", WorkTypeStatus.ClosedRejected),
                testWorkType("debris", WorkTypeStatus.OpenNeedsFollowUp),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun existingWorksiteNewWorkTypeFormDataDifferentStatus() {
        val worksite = testWorksite()
            .copy(
                workTypes = listOf(
                    testWorkType("tarp", WorkTypeStatus.OpenAssigned),
                ),
            )
        val formFieldData = makeFormFieldData(
            mapOf(
                "tree_info" to WorksiteFormValue.trueValue,
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "tarping_info" to WorksiteFormValue.trueValue,
            ),
            mapOf(
                "tarp" to WorkTypeStatus.ClosedIncomplete,
                "trees" to WorkTypeStatus.ClosedIncomplete,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("tarp", WorkTypeStatus.ClosedIncomplete),
                testWorkType("trees", WorkTypeStatus.ClosedIncomplete),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }

    @Test
    fun deduplicateWorkTypes() {
        val worksite = testWorksite()
            .copy(
                workTypes = listOf(
                    testWorkType("tarp", WorkTypeStatus.OpenAssigned),
                    testWorkType("mold_remediation", WorkTypeStatus.OpenPartiallyCompleted),
                    testWorkType("trees", WorkTypeStatus.OpenUnresponsive, 84),
                    testWorkType("tarp", WorkTypeStatus.ClosedDuplicate, 55),
                    testWorkType("trees", WorkTypeStatus.ClosedRejected, 11),
                    testWorkType("trees", WorkTypeStatus.OpenPartiallyCompleted, 33),
                ),
            )
        val formFieldData = makeFormFieldData(
            mapOf(
                "debris_info" to WorksiteFormValue.trueValue,
                "tree_info" to WorksiteFormValue.trueValue,
                "vegitative_debris_removal" to WorksiteFormValue.trueValue,
                "tarping_info" to WorksiteFormValue.trueValue,
            ),
            mapOf(
                "tarp" to WorkTypeStatus.ClosedIncomplete,
                "debris" to WorkTypeStatus.ClosedDoneByOthers,
            ),
        )

        val updatedWorksite = worksite
            .updateWorkTypeStatuses(
                workTypeLookup,
                formFieldData,
            )
        val expectedWorksite = worksite.copy(
            workTypes = listOf(
                testWorkType("tarp", WorkTypeStatus.ClosedIncomplete, 55),
                testWorkType("trees", WorkTypeStatus.OpenUnassigned, 84),
                testWorkType("debris", WorkTypeStatus.ClosedDoneByOthers),
            ),
        )
        assertEquals(
            expectedWorksite.equalizeCreatedAt(),
            updatedWorksite.equalizeCreatedAt(),
        )
    }
}

internal fun testWorksite(
    flags: List<WorksiteFlag>? = null,
    formData: Map<String, WorksiteFormValue>? = null,
    keyWorkType: WorkType? = null,
    workTypes: List<WorkType> = emptyList(),
    id: Long = 1,
    incidentId: Long = 2,
    networkId: Long = 3,
) = Worksite(
    id = id,
    address = "address",
    autoContactFrequencyT = AutoContactFrequency.NotOften.literal,
    caseNumber = "case-number",
    city = "city",
    county = "county",
    createdAt = null,
    favoriteId = null,
    flags = flags,
    formData = formData,
    incidentId = incidentId,
    keyWorkType = keyWorkType,
    latitude = 0.0,
    longitude = 0.0,
    name = "name",
    networkId = networkId,
    phone1 = "phone",
    phone1Notes = "phone-notes",
    phone2 = "second-phone",
    phone2Notes = "phone2-notes",
    postalCode = "postal-code",
    reportedBy = null,
    state = "state",
    svi = null,
    updatedAt = null,
    workTypes = workTypes,
)

private fun testWorkType(
    workTypeLiteral: String,
    status: WorkTypeStatus = WorkTypeStatus.OpenUnassigned,
    id: Long = 0,
) = WorkType(
    id = id,
    statusLiteral = status.literal,
    workTypeLiteral = workTypeLiteral,
)

private fun testFormField(
    group: String,
    fieldKey: String,
    parentKey: String,
    selectToggleWorkType: String = "",
) = IncidentFormField(
    label = "",
    htmlType = "",
    group = group,
    help = "",
    placeholder = "",
    validation = "",
    valuesDefault = null,
    values = emptyMap(),
    isCheckboxDefaultTrue = false,
    recurDefault = "",
    isRequired = false,
    isReadOnly = false,
    isReadOnlyBreakGlass = false,
    labelOrder = 0,
    listOrder = 0,
    isInvalidated = false,
    fieldKey = fieldKey,
    parentKey = parentKey,
    selectToggleWorkType = selectToggleWorkType,
)

private val now = Clock.System.now()

internal fun List<WorksiteFlag>.flagsEqualizeCreatedAt() = map { flag ->
    flag.copy(createdAt = now)
}

private fun List<WorkType>.workTypesEqualizeCreatedAt() = map { workType ->
    workType.copy(createdAt = now)
}

private fun Worksite.equalizeCreatedAt() = copy(
    flags = flags?.flagsEqualizeCreatedAt(),
    workTypes = workTypes.workTypesEqualizeCreatedAt(),
)
