package com.crisiscleanup.core.database.dao

import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.TestUtil
import com.crisiscleanup.core.database.WorksiteTestUtil.testIncidents
import com.crisiscleanup.core.database.model.IncidentFormFieldEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.IncidentFormField
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class IncidentDaoFormFieldTest {
    private lateinit var db: CrisisCleanupDatabase

    private lateinit var incidentDao: IncidentDao
    private lateinit var incidentDaoPlus: IncidentDaoPlus

    @Before
    fun createDb() {
        db = TestUtil.getDatabase()
        incidentDao = db.incidentDao()
        incidentDaoPlus = IncidentDaoPlus(db)
    }

    @Before
    fun seedDb() = runTest {
        incidentDao.upsertIncidents(testIncidents)
    }

    private suspend fun getIncidentFormFields(incidentId: Long) =
        incidentDao.getFormFieldsIncident(incidentId)
            ?.asExternalModel()?.formFields?.sortedWith { a, b -> a.fieldKey.compareTo(b.fieldKey) }

    @Test
    fun upsetFormFields() = runTest {
        incidentDaoPlus.updateFormFields(
            23,
            listOf(
                testFormFieldEntity(23, "field-a"),
                testFormFieldEntity(23, "field-b"),
                testFormFieldEntity(23, "field-c"),
            )
        )

        val expected1 = listOf(
            testFormField("field-a"),
            testFormField("field-b"),
            testFormField("field-c"),
        )
        assertEquals(emptyList(), getIncidentFormFields(1))
        assertEquals(expected1, getIncidentFormFields(23))
        assertEquals(emptyList(), getIncidentFormFields(456))

        // Update empties do not change anything
        incidentDaoPlus.updateFormFields(456, emptyList())
        assertEquals(emptyList(), getIncidentFormFields(1))
        assertEquals(expected1, getIncidentFormFields(23))
        assertEquals(emptyList(), getIncidentFormFields(456))

        // Update where not specified invalidates
        incidentDaoPlus.updateFormFields(
            23,
            listOf(
                testFormFieldEntity(23, "field-d"),
                testFormFieldEntity(23, "field-b", label = "label-b"),
                testFormFieldEntity(23, "field-c"),
            )
        )
        val expected2 = expected1.toMutableList().also {
            it.add(testFormField("field-d"))
            it[1] = it[1].copy(label = "label-b")
            it.removeAt(0)
        }
        assertEquals(emptyList(), getIncidentFormFields(1))
        assertEquals(expected2, getIncidentFormFields(23))
        assertEquals(emptyList(), getIncidentFormFields(456))
    }
}

private fun testFormFieldEntity(
    incidentId: Long,
    fieldKey: String,
    label: String = "label",
) = IncidentFormFieldEntity(
    incidentId = incidentId,
    label = label,
    htmlType = "html-type",
    dataGroup = "data-group",
    help = null,
    placeholder = null,
    readOnlyBreakGlass = false,
    valuesDefaultJson = null,
    isCheckboxDefaultTrue = null,
    orderLabel = -1,
    validation = null,
    recurDefault = null,
    valuesJson = null,
    isRequired = null,
    isReadOnly = null,
    listOrder = -1,
    isInvalidated = false,
    fieldKey = fieldKey,
    parentKeyNonNull = "",
    selectToggleWorkType = null,
)

private fun testFormField(fieldKey: String) = IncidentFormField(
    label = "label",
    htmlType = "html-type",
    group = "data-group",
    help = "",
    placeholder = "",
    validation = "",
    isReadOnlyBreakGlass = false,
    valuesDefault = emptyMap(),
    values = emptyMap(),
    isCheckboxDefaultTrue = false,
    recurDefault = "",
    isRequired = false,
    isReadOnly = false,
    labelOrder = -1,
    listOrder = -1,
    isInvalidated = false,
    fieldKey = fieldKey,
    parentKey = "",
    selectToggleWorkType = "",
)