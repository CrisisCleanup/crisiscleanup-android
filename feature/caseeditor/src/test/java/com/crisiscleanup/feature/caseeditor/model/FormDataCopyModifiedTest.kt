package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.network.model.DynamicValue
import org.junit.Test
import kotlin.test.assertEquals

class FormDataCopyModifiedTest {
    @Test
    fun noChanges() {
        val worksite = makeTestWorksiteFormData()

        assertEquals(worksite.formData, worksite.copyModifiedFormData(emptyMap()))
        assertEquals(
            worksite.formData, worksite.copyModifiedFormData(
                mapOf(
                    "a" to DynamicValue(""),
                    "b" to DynamicValue("", true),
                )
            )
        )
    }

    @Test
    fun booleanChanges() {
        val worksite = makeTestWorksiteFormData(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
            )
        )

        // Remove only
        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "c" to WorksiteFormValue(true, "", false),
            ),
            worksite.copyModifiedFormData(
                mapOf("b" to DynamicValue("", isBoolean = true, false)),
            )
        )

        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "b" to DynamicValue("", isBoolean = true, false),
                    "c" to DynamicValue("", isBoolean = true, false)
                )
            )
        )

        // Change/add only
        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", true),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "c" to DynamicValue("", isBoolean = true, true),
                )
            )
        )

        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(isBoolean = true, "", true),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "d" to DynamicValue("", isBoolean = true, true),
                )
            )
        )

        // Changes
        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(true, "", true),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "b" to DynamicValue("", isBoolean = true, true),
                    "c" to DynamicValue("", isBoolean = true, false),
                )
            )
        )

        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "c" to WorksiteFormValue(true, "", true),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "b" to DynamicValue("", isBoolean = true, false),
                    "c" to DynamicValue("", isBoolean = true, true),
                )
            )
        )
    }

    @Test
    fun stringChanges() {
        val worksite = makeTestWorksiteFormData(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "early"),
            )
        )

        // Remove only
        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "d" to DynamicValue(""),
                )
            )
        )

        assertEquals(
            mapOf(
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "early"),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "a" to DynamicValue(""),
                )
            )
        )

        assertEquals(
            mapOf(
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "a" to DynamicValue(""),
                    "d" to DynamicValue(""),
                )
            )
        )

        // Change/add
        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, "change-a"),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "early"),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "a" to DynamicValue("change-a"),
                )
            )
        )

        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, "change-a"),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "early"),
                "e" to WorksiteFormValue(false, "add-e")
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "a" to DynamicValue("change-a"),
                    "e" to DynamicValue("add-e"),
                )
            )
        )

        // Changes
        assertEquals(
            mapOf(
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "change-d"),
                "e" to WorksiteFormValue(false, "add-e")
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    "a" to DynamicValue(""),
                    "d" to DynamicValue("change-d "),
                    "e" to DynamicValue("add-e"),
                )
            )
        )
    }

    @Test
    fun manyChanges() {
        val worksite = makeTestWorksiteFormData(
            mapOf(
                "a" to WorksiteFormValue(false, "a"),
                "b" to WorksiteFormValue(true, "", true),
                "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "Diff eren"),
                "e" to WorksiteFormValue(false, "early"),
                "f" to WorksiteFormValue(true, "", true),
                "g" to WorksiteFormValue(false, " g "),
            )
        )

        // Remove only
        assertEquals(
            mapOf(
                "a" to WorksiteFormValue(false, "a-ttackon"),
                "b" to WorksiteFormValue(true, "", true),
                // Removed
                // "c" to WorksiteFormValue(true, "", false),
                "d" to WorksiteFormValue(false, "Diff eren"),
                "e" to WorksiteFormValue(false, "change-e"),
                "f" to WorksiteFormValue(true, "", true),
                "g" to WorksiteFormValue(false, "g"),
                "i" to WorksiteFormValue(false, "irrigation"),
                "m" to WorksiteFormValue(true, "", true),
            ),
            worksite.copyModifiedFormData(
                mapOf(
                    // Add
                    "i" to DynamicValue("\tirrigation \n"),
                    // Remove
                    "c" to DynamicValue("", true),
                    // Change
                    "e" to DynamicValue("change-e"),
                    // Change
                    "a" to DynamicValue("a-ttackon"),
                    // No-op
                    "k" to DynamicValue(""),
                    // No change
                    "g" to DynamicValue("g"),
                    // No-op
                    "j" to DynamicValue("", isBoolean = true, false),
                    // No-op
                    "l" to DynamicValue("", true),
                    // Add
                    "m" to DynamicValue("morrey", isBoolean = true, true),
                )
            )
        )
    }
}