package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.core.testing.model.makeTestWorksite
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class WorksiteFormDataChangeTest {
    @Test
    fun noChange_noFields() {
        val worksiteNullFormData = makeTestWorksiteFormData()
        assertFalse(worksiteNullFormData.seekChange(emptyMap()))

        val worksiteEmptyFormData = makeTestWorksiteFormData(emptyMap())
        assertFalse(worksiteEmptyFormData.seekChange(emptyMap()))
    }

    @Test
    fun noChange_defaultFields() {
        val defaultDynamicData = mapOf(
            "a" to DynamicValue(""),
            "b" to DynamicValue("", isBoolean = true),
        )

        val worksiteNullFormData = makeTestWorksiteFormData()
        assertFalse(worksiteNullFormData.seekChange(defaultDynamicData))

        val worksiteEmptyFormData = makeTestWorksiteFormData(emptyMap())
        assertFalse(worksiteEmptyFormData.seekChange(defaultDynamicData))
    }

    @Test
    fun noChange_equivalentFields() {
        val worksite = makeTestWorksiteFormData(
            mapOf(
                "a" to WorksiteFormValue(false, ""),
                "b" to WorksiteFormValue(false, " b"),
                "c" to WorksiteFormValue(true, "doesn't matter", false),
                "d" to WorksiteFormValue(true, "irrelevant", true),
            ),
        )

        val dynamicData = mapOf(
            "a" to DynamicValue(""),
            // Compares on trimmed string value
            "b" to DynamicValue("b "),
            // Boolean does not compare string
            "c" to DynamicValue("ignore", isBoolean = true, false),
            // Boolean does not compare string
            "d" to DynamicValue("something else", isBoolean = true, true),
            // Default false is equivalent to not defined
            "e" to DynamicValue("", isBoolean = true, false),
            // Default white space string is equivalent to not defined
            "f" to DynamicValue("", isBoolean = false),
        )

        assertFalse(worksite.seekChange(dynamicData))
    }

    @Test
    fun booleanFieldChanges() {
        val worksiteNull = makeTestWorksiteFormData()
        val worksiteEmpty = makeTestWorksiteFormData(emptyMap())
        val worksiteFalse = makeTestWorksiteFormData(
            mapOf("a" to WorksiteFormValue(true, "", false)),
        )
        val worksiteTrue = makeTestWorksiteFormData(
            mapOf("a" to WorksiteFormValue(true, "", true)),
        )

        val dynamicDataEmpty = emptyMap<String, DynamicValue>()
        val dynamicDataFalse = mapOf("a" to DynamicValue("", isBoolean = true, false))
        val dynamicDataTrue = mapOf("a" to DynamicValue("", isBoolean = true, true))

        assertTrue(worksiteNull.seekChange(dynamicDataTrue))
        assertTrue(worksiteEmpty.seekChange(dynamicDataTrue))
        assertTrue(worksiteFalse.seekChange(dynamicDataTrue))

        // Empty reference results in no comparisons
        assertFalse(worksiteTrue.seekChange(dynamicDataEmpty))
        assertTrue(worksiteTrue.seekChange(dynamicDataFalse))
    }

    @Test
    fun stringFieldChange() {
        val worksiteNull = makeTestWorksiteFormData()
        val worksiteEmpty = makeTestWorksiteFormData(emptyMap())
        val worksiteWhitespace = makeTestWorksiteFormData(
            mapOf("a" to WorksiteFormValue(false, "  ")),
        )
        val worksiteLowercase = makeTestWorksiteFormData(
            mapOf("a" to WorksiteFormValue(false, "animal")),
        )
        val worksiteCaps = makeTestWorksiteFormData(
            mapOf("a" to WorksiteFormValue(false, "ANIMAL")),
        )

        val dynamicDataEmpty = emptyMap<String, DynamicValue>()
        val dynamicDataWhitespace = mapOf("a" to DynamicValue("  "))
        val dynamicDataLowercase = mapOf("a" to DynamicValue(" animal "))
        val dynamicDataUppercase = mapOf("a" to DynamicValue("Animal"))
        val dynamicDataDifferentText = mapOf("a" to DynamicValue("different text"))

        assertFalse(worksiteNull.seekChange(dynamicDataEmpty))
        assertFalse(worksiteNull.seekChange(dynamicDataWhitespace))
        assertTrue(worksiteNull.seekChange(dynamicDataLowercase))
        assertTrue(worksiteNull.seekChange(dynamicDataUppercase))
        assertTrue(worksiteNull.seekChange(dynamicDataDifferentText))

        assertFalse(worksiteEmpty.seekChange(dynamicDataEmpty))
        assertFalse(worksiteEmpty.seekChange(dynamicDataWhitespace))
        assertTrue(worksiteEmpty.seekChange(dynamicDataLowercase))
        assertTrue(worksiteEmpty.seekChange(dynamicDataUppercase))
        assertTrue(worksiteEmpty.seekChange(dynamicDataDifferentText))

        assertFalse(worksiteWhitespace.seekChange(dynamicDataEmpty))
        assertFalse(worksiteWhitespace.seekChange(dynamicDataWhitespace))
        assertTrue(worksiteWhitespace.seekChange(dynamicDataLowercase))
        assertTrue(worksiteWhitespace.seekChange(dynamicDataUppercase))
        assertTrue(worksiteWhitespace.seekChange(dynamicDataDifferentText))

        assertFalse(worksiteLowercase.seekChange(dynamicDataEmpty))
        assertTrue(worksiteLowercase.seekChange(dynamicDataWhitespace))
        assertFalse(worksiteLowercase.seekChange(dynamicDataLowercase))
        assertTrue(worksiteLowercase.seekChange(dynamicDataUppercase))
        assertTrue(worksiteLowercase.seekChange(dynamicDataDifferentText))

        assertFalse(worksiteCaps.seekChange(dynamicDataEmpty))
        assertTrue(worksiteCaps.seekChange(dynamicDataWhitespace))
        assertTrue(worksiteCaps.seekChange(dynamicDataLowercase))
        assertTrue(worksiteCaps.seekChange(dynamicDataUppercase))
        assertTrue(worksiteCaps.seekChange(dynamicDataDifferentText))
    }
}

val dateA = Clock.System.now().minus(1.days)
internal fun makeTestWorksiteFormData(
    formData: Map<String, WorksiteFormValue>? = null,
) = makeTestWorksite(
    dateA,
    dateA,
    null,
    formData,
)
