package com.crisiscleanup.core.network.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkFlagsFormDataTest {
    @Test
    fun getFlagsFormDataSuccess() {
        val result =
            TestUtil.decodeResource<NetworkFlagsFormDataResult>("/getFlagsFormDataSuccess.json")

        assertNull(result.errors)
        assertEquals(20, result.count)

        val entry = result.results!![2]

        val expected = NetworkFlagsFormData(
            id = 229138,
            caseNumber = "VW2UVP",
            formData = listOf(
                KeyDynamicValuePair(
                    key = "debris_info",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "debris_description",
                    value = DynamicValue(valueString = "shed has well and was damaged  "),
                ),
                KeyDynamicValuePair(
                    key = "unsalvageable_structure",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "vegitative_debris_removal",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "habitable",
                    value = DynamicValue(valueString = "formOptions.yes"),
                ),
                KeyDynamicValuePair(
                    key = "residence_type",
                    value = DynamicValue(valueString = "formOptions.primary_living_in_home"),
                ),
                KeyDynamicValuePair(
                    key = "dwelling_type",
                    value = DynamicValue(valueString = "formOptions.mobile_home"),
                ),
                KeyDynamicValuePair(
                    key = "work_without_resident",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
                KeyDynamicValuePair(
                    key = "tree_info",
                    value = DynamicValue(valueString = "", isBoolean = true, valueBoolean = true),
                ),
            ),
            flags = emptyList(),
            phone1 = "1234567890",
            reportedBy = 861,
        )
        assertEquals(expected, entry)
    }
}
