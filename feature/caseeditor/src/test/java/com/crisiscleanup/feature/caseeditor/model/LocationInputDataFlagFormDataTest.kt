package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.testing.model.makeTestWorksite
import com.crisiscleanup.core.testing.model.makeTestWorksiteFlag
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.datetime.Clock
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class LocationInputDataFlagFormDataTest {
    @MockK
    lateinit var translator: KeyResourceTranslator

    private val now = Clock.System.now()
    private val createdAtA = now.minus(2.days)
    private val createdAtB = now.plus(39.seconds)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun updateCase_noChangeInWrongLocation() {
        val baseWorksite = worksiteFlagsTest(baseFlags)
        val locationInputData = LocationInputData(translator, baseWorksite)

        val noChangeUpdate = locationInputData.updateCase()
        assertEquals(baseWorksite, noChangeUpdate)
    }

    @Test
    fun updateCase_noChangeInWrongLocation_nullFlags() {
        val nullFlagsWorksite = worksiteFlagsTest(null)
        val nullFlagsInputData = LocationInputData(translator, nullFlagsWorksite)
        val nullFlagsUpdate = nullFlagsInputData.updateCase()
        assertEquals(nullFlagsWorksite, nullFlagsUpdate)
    }

    @Test
    fun updateCase_noChangeInWrongLocation_emptyFlags() {
        val emptyFlagsWorksite = worksiteFlagsTest(emptyList())
        val emptyFlagsInputData = LocationInputData(translator, emptyFlagsWorksite)
        val emptyFlagsUpdate = emptyFlagsInputData.updateCase()
        assertEquals(emptyFlagsWorksite, emptyFlagsUpdate)
    }

    private fun assertUpdatedWrongLocationFlag(
        worksite: Worksite,
        updated: Worksite?,
    ) {
        val expected = worksite.copy(
            flags = updated?.flags,
        )
        assertEquals(expected, updated)

        val actual = updated!!.flags
        val expectedFlagCount = (worksite.flags?.size ?: 0) + 1
        assertEquals(expectedFlagCount, actual?.size)
        val lastFlag = actual?.lastOrNull()
        val expectedFlag = WorksiteFlag(
            0,
            "",
            lastFlag!!.createdAt,
            false,
            "",
            "flag.worksite_wrong_location",
            "",
            "",
        )
        assertEquals(expectedFlag, lastFlag)
    }

    @Test
    fun updateCase_isWrongLocation_multipleFlags() {
        val worksite = worksiteFlagsTest(
            listOf(
                makeTestWorksiteFlag(createdAtA, "reason-a"),
                makeTestWorksiteFlag(createdAtB, "reason-b"),
            )
        )

        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.hasWrongLocation = true

        val updated = locationInputData.updateCase()

        assertUpdatedWrongLocationFlag(worksite, updated)
    }

    @Test
    fun updateCase_isWrongLocation_singleFlag() {
        val worksite = worksiteFlagsTest(
            listOf(
                makeTestWorksiteFlag(createdAtA, "reason-a"),
            )
        )

        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.hasWrongLocation = true

        val updated = locationInputData.updateCase()

        assertUpdatedWrongLocationFlag(worksite, updated)
    }

    @Test
    fun updateCase_isWrongLocation_nullFlags() {
        val worksite = worksiteFlagsTest(null)

        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.hasWrongLocation = true

        val updated = locationInputData.updateCase()

        assertUpdatedWrongLocationFlag(worksite, updated)
    }

    @Test
    fun updateCase_isWrongLocation_emptyFlags() {
        val worksite = worksiteFlagsTest(emptyList())

        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.hasWrongLocation = true

        val updated = locationInputData.updateCase()

        assertUpdatedWrongLocationFlag(worksite, updated)
    }

    private fun assertRemoveWrongLocationFlag(
        worksite: Worksite,
        updated: Worksite?,
        expectedFlags: List<WorksiteFlag>?,
    ) {
        val expected = worksite.copy(
            flags = updated?.flags,
        )
        assertEquals(expected, updated)

        assertEquals(expectedFlags, updated!!.flags)
    }

    @Test
    fun updateCase_removeWrongLocation_multipleFlags() {
        val worksite = worksiteFlagsTest(
            listOf(
                makeTestWorksiteFlag(createdAtA, "flag.worksite_wrong_location"),
                makeTestWorksiteFlag(createdAtB, "reason-b"),
            )
        )

        val locationInputData = LocationInputData(translator, worksite)
        assertTrue(locationInputData.hasWrongLocation)

        locationInputData.hasWrongLocation = false

        val updated = locationInputData.updateCase()

        assertRemoveWrongLocationFlag(
            worksite, updated, listOf(
                makeTestWorksiteFlag(createdAtB, "reason-b"),
            )
        )
    }

    @Test
    fun updateCase_removeWrongLocation_singleFlag() {
        val worksite = worksiteFlagsTest(
            listOf(
                makeTestWorksiteFlag(createdAtA, "flag.worksite_wrong_location"),
            )
        )

        val locationInputData = LocationInputData(translator, worksite)
        assertTrue(locationInputData.hasWrongLocation)

        locationInputData.hasWrongLocation = false

        val updated = locationInputData.updateCase()

        assertRemoveWrongLocationFlag(worksite, updated, null)
    }

    @Test
    fun updateCase_noChangeInFormData() {
        val worksite = worksiteFormTest(baseFormData)
        val locationInputData = LocationInputData(translator, worksite)

        val update = locationInputData.updateCase()
        assertEquals(worksite, update)
    }

    @Test
    fun updateCase_noChangeInFormData_nullForm() {
        val worksite = worksiteFormTest(null)
        val locationInputData = LocationInputData(translator, worksite)

        val update = locationInputData.updateCase()
        assertEquals(worksite, update)
    }

    @Test
    fun updateCase_noChangeInFormData_emptyForm() {
        val worksite = worksiteFormTest(emptyMap())
        val locationInputData = LocationInputData(translator, worksite)

        val update = locationInputData.updateCase()
        assertEquals(worksite, update)
    }

    private fun assertUpdatedCrossStreet(
        worksite: Worksite,
        updated: Worksite?,
        crossStreet: String = "cross-street",
    ) {
        val expected = worksite.copy(
            formData = updated?.formData,
        )
        assertEquals(expected, updated)

        val actual = updated!!.formData
        val expectedFormDataCount = (worksite.formData?.size ?: 0) + 1
        assertEquals(expectedFormDataCount, actual?.size)
        val formDataMap = worksite.formData?.toMutableMap() ?: mutableMapOf()
        val expectedFormData = formDataMap.apply {
            put("cross_street", WorksiteFormValue(valueString = crossStreet))
        }
        assertEquals(expectedFormData, actual)
    }

    @Test
    fun updateCase_hasCrossStreet_multipleFormData() {
        val worksite = worksiteFormTest(
            mapOf(
                "form-data-a" to WorksiteFormValue(valueString = "a"),
                "form-data-b" to WorksiteFormValue(
                    isBoolean = true,
                    valueString = "",
                    valueBoolean = false
                ),
            )
        )
        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.crossStreetNearbyLandmark = "cross-street"

        val update = locationInputData.updateCase()

        assertUpdatedCrossStreet(worksite, update)
    }

    @Test
    fun updateCase_hasCrossStreet_singleFormData() {
        val worksite = worksiteFormTest(
            mapOf(
                "form-data-a" to WorksiteFormValue(valueString = "a"),
            )
        )
        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.crossStreetNearbyLandmark = "cross-street"

        val update = locationInputData.updateCase()

        assertUpdatedCrossStreet(worksite, update)
    }

    @Test
    fun updateCase_hasCrossStreet_emptyFormData() {
        val worksite = worksiteFormTest(emptyMap())
        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.crossStreetNearbyLandmark = "cross-street"

        val update = locationInputData.updateCase()

        assertUpdatedCrossStreet(worksite, update)
    }

    @Test
    fun updateCase_hasCrossStreet_nullFormData() {
        val worksite = worksiteFormTest(null)
        val locationInputData = LocationInputData(translator, worksite)
        locationInputData.crossStreetNearbyLandmark = "cross-street"

        val update = locationInputData.updateCase()

        assertUpdatedCrossStreet(worksite, update)
    }

    private fun assertRemoveCrossStreet(
        worksite: Worksite,
        updated: Worksite?,
        expectedFormData: Map<String, WorksiteFormValue>?,
    ) {
        val expected = worksite.copy(
            formData = updated?.formData,
        )
        assertEquals(expected, updated)

        assertEquals(expectedFormData, updated!!.formData)
    }

    @Test
    fun updateCase_removeCrossStreet_multipleFormData() {
        val worksite = worksiteFormTest(
            mapOf(
                "cross_street" to WorksiteFormValue(valueString = "cross-street-a"),
                "form-data-b" to WorksiteFormValue(
                    isBoolean = true,
                    valueString = "",
                    valueBoolean = false
                ),
            )
        )
        val locationInputData = LocationInputData(translator, worksite)
        assertEquals("cross-street-a", locationInputData.crossStreetNearbyLandmark)

        locationInputData.crossStreetNearbyLandmark = " "

        val update = locationInputData.updateCase()

        assertRemoveCrossStreet(
            worksite, update, mapOf(
                "form-data-b" to WorksiteFormValue(
                    isBoolean = true,
                    valueString = "",
                    valueBoolean = false
                ),
            )
        )
    }

    @Test
    fun updateCase_removeCrossStreet_singleFormData() {
        val worksite = worksiteFormTest(
            mapOf(
                "cross_street" to WorksiteFormValue(valueString = "cross-street-a"),
            )
        )
        val locationInputData = LocationInputData(translator, worksite)
        assertEquals("cross-street-a", locationInputData.crossStreetNearbyLandmark)

        locationInputData.crossStreetNearbyLandmark = " "

        val update = locationInputData.updateCase()

        assertRemoveCrossStreet(worksite, update, null)
    }
}

private val prevCreatedAt = Clock.System.now().minus(1.days)
private val baseFlags = listOf(
    makeTestWorksiteFlag(
        id = 84,
        createdAt = prevCreatedAt,
        isHighPriority = false,
        reasonT = "worksite-flag",
    )
)
private val baseFormData = mapOf(
    "boolean-data" to WorksiteFormValue(isBoolean = true, valueString = "", valueBoolean = true),
    "string-data" to WorksiteFormValue(valueString = "form-data"),
)

private fun worksiteFlagsTest(flags: List<WorksiteFlag>?) = makeTestWorksite(
    prevCreatedAt,
    prevCreatedAt,
    flags,
    baseFormData,
)

private fun worksiteFormTest(formData: Map<String, WorksiteFormValue>?) = makeTestWorksite(
    prevCreatedAt,
    prevCreatedAt,
    baseFlags,
    formData
)