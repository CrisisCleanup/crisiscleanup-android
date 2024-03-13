package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkFlagsFormDataTest {
    @Test
    fun getWorksitesCount() {
        val result =
            TestUtil.decodeResource<NetworkFlagsFormDataResult>("/getFlagsFormDataSuccess.json")

        assertNull(result.errors)
        assertEquals(20, result.count)

        val entry = result.results!![2]

        val expected = NetworkFlagsFormData(
            id = 220667,
            caseNumber = "V2026",
            formData = listOf(
                KeyDynamicValuePair(
                    key = "cross_street",
                    value = DynamicValue(valueString = "Easy St"),
                ),
                KeyDynamicValuePair(
                    key = "time_to_call",
                    value = DynamicValue(valueString = "anytime"),
                ),
                KeyDynamicValuePair(
                    key = "older_than_60",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "special_needs",
                    value = DynamicValue(valueString = "disabled"),
                ),
                KeyDynamicValuePair(
                    key = "primary_language",
                    value = DynamicValue(valueString = "formOptions.english"),
                ),
                KeyDynamicValuePair(
                    key = "residence_type",
                    value = DynamicValue(valueString = "formOptions.primary_living_in_home"),
                ),
                KeyDynamicValuePair(
                    key = "dwelling_type",
                    value = DynamicValue(valueString = "formOptions.house"),
                ),
                KeyDynamicValuePair(
                    key = "rent_or_own",
                    value = DynamicValue(valueString = "formOptions.rent"),
                ),
                KeyDynamicValuePair(
                    key = "insurance_home_rent",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "water_status",
                    value = DynamicValue(valueString = "formOptions.on"),
                ),
                KeyDynamicValuePair(
                    key = "power_status",
                    value = DynamicValue(valueString = "formOptions.off"),
                ),
                KeyDynamicValuePair(
                    key = "tree_info",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "num_wide_trees",
                    value = DynamicValue(valueString = "formOptions.four"),
                ),
                KeyDynamicValuePair(
                    key = "debris_info",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "debris_description",
                    value = DynamicValue(valueString = "carport gone as well as fences"),
                ),
                KeyDynamicValuePair(
                    key = "habitable",
                    value = DynamicValue(valueString = "formOptions.yes"),
                ),
                KeyDynamicValuePair(
                    key = "debris_status",
                    value = DynamicValue(valueString = "formOptions.piled_on_public_right_of_way"),
                ),
                KeyDynamicValuePair(
                    key = "prepared_by",
                    value = DynamicValue(valueString = "Mike DeLoach"),
                ),
                KeyDynamicValuePair(
                    key = "work_without_resident",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
            ),
            flags = emptyList(),
        )
        assertEquals(expected, entry)
    }
}