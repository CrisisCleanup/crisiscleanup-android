package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.*
import com.crisiscleanup.feature.caseeditor.util.resolveModifiedWorkTypes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class ResolveModifiedWorkTypesTest {
    private val workTypeLookup = mapOf(
        "form-field-ash-a" to WorkTypeType.Ash.name,
        "form-field-ash-b" to WorkTypeType.Ash.name,
        "form-field-chimney-a" to WorkTypeType.ChimneyCapping.name,
        "form-field-chimney-b" to WorkTypeType.ChimneyCapping.name,
        "form-field-chimney-c" to WorkTypeType.ChimneyCapping.name,
        "form-field-trees-a" to WorkTypeType.Trees.name,
        "form-field-trees-b" to WorkTypeType.Trees.name,
        "form-field-sandbag-a" to WorkTypeType.Sandbagging.name,
    )

    private val workTypeAsh = testWorkType(WorkTypeType.Ash, id = 1)
    private val workTypeChimney = testWorkType(WorkTypeType.ChimneyCapping, id = 2)
    private val workTypeTrees = testWorkType(WorkTypeType.Trees, id = 3)

    private val worksiteA = testWorksite(
        listOf(
            workTypeTrees,
            workTypeAsh,
            workTypeChimney,
        ),
        workTypeChimney,
    )

    private val emptyFormValue = WorksiteFormValue(valueString = "")

    @Test
    fun noChanges() {
        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-chimney-b" to emptyFormValue,
                "form-field-trees-a" to emptyFormValue,
                "form-field-ash-a" to emptyFormValue,
                "form-field-chimney-a" to emptyFormValue,
                "form-field-trees-b" to emptyFormValue,
                "form-field-ash-b" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            worksiteA,
            modifiedWorksite,
        )

        assertEquals(worksiteA.workTypes, actualWorkTypes)
        assertEquals(worksiteA.keyWorkType, actualKeyWorkType)
    }

    @Test
    fun emptyAdd() {
        val emptyWorksite = testWorksite()
        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-ash-a" to emptyFormValue,
                "form-field-chimney-b" to emptyFormValue,
                "form-field-trees-b" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            emptyWorksite,
            modifiedWorksite,
            now,
        )

        val expectedWorkTypes = listOf(
            testWorkType(WorkTypeType.Ash, now),
            testWorkType(WorkTypeType.ChimneyCapping, now),
            testWorkType(WorkTypeType.Trees, now),
        )
        assertEquals(expectedWorkTypes, actualWorkTypes)
        assertEquals(expectedWorkTypes[0], actualKeyWorkType)
    }

    @Test
    fun deleteAll() {
        val modifiedWorksite = testWorksite()

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            worksiteA,
            modifiedWorksite,
        )

        assertEquals(emptyList(), actualWorkTypes)
        assertEquals(null, actualKeyWorkType)
    }

    @Test
    fun addNew() {
        val initialWorksite = testWorksite(
            listOf(
                workTypeTrees,
                workTypeChimney,
            ),
            workTypeTrees,
        )

        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-chimney-b" to emptyFormValue,
                "form-field-trees-a" to emptyFormValue,
                "form-field-ash-a" to emptyFormValue,
                "form-field-chimney-a" to emptyFormValue,
                "form-field-trees-b" to emptyFormValue,
                "form-field-ash-b" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            initialWorksite,
            modifiedWorksite,
            createdAtB,
        )

        val expectedWorkTypes = listOf(
            workTypeChimney,
            workTypeTrees,
            testWorkType(WorkTypeType.Ash, createdAtB),
        )
        assertEquals(expectedWorkTypes, actualWorkTypes)
        assertEquals(initialWorksite.keyWorkType, actualKeyWorkType)
    }

    @Test
    fun deleteKeyWorkType() {
        val initialWorksite = testWorksite(
            listOf(
                workTypeTrees,
                workTypeChimney,
            ),
            workTypeTrees,
        )

        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-ash-a" to emptyFormValue,
                "form-field-trees-a" to emptyFormValue,
                "form-field-trees-b" to emptyFormValue,
                "form-field-ash-b" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            initialWorksite,
            modifiedWorksite,
            createdAtB,
        )

        val expectedWorkTypes = listOf(
            testWorkType(WorkTypeType.Ash, createdAtB),
            workTypeTrees,
        )
        assertEquals(expectedWorkTypes, actualWorkTypes)
        assertEquals(workTypeTrees, actualKeyWorkType)
    }

    @Test
    fun deleteInitialWorkTypes() {
        val initialWorksite = testWorksite(
            listOf(
                workTypeTrees,
                workTypeChimney,
            ),
            workTypeTrees,
        )

        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-sandbag-a" to emptyFormValue,
                "form-field-ash-a" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            initialWorksite,
            modifiedWorksite,
            createdAtB,
        )

        val expectedWorkTypes = listOf(
            testWorkType(WorkTypeType.Sandbagging, createdAtB),
            testWorkType(WorkTypeType.Ash, createdAtB),
        )
        assertEquals(expectedWorkTypes, actualWorkTypes)
        assertEquals(expectedWorkTypes[1], actualKeyWorkType)
    }

    @Test
    fun modifiedSingleWorkType() {
        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-sandbag-a" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            worksiteA,
            modifiedWorksite,
            createdAtB,
        )

        val expectedWorkTypes = listOf(
            testWorkType(WorkTypeType.Sandbagging, createdAtB),
        )
        assertEquals(expectedWorkTypes, actualWorkTypes)
        assertEquals(expectedWorkTypes[0], actualKeyWorkType)
    }

    @Test
    fun noChangeToKeyWorkType() {
        val initialWorksite = testWorksite(
            listOf(
                workTypeAsh,
                workTypeTrees,
                workTypeChimney,
            ),
            workTypeChimney,
        )

        val modifiedWorksite = testWorksite(
            formData = mapOf(
                "form-field-sandbag-a" to emptyFormValue,
                "form-field-chimney-a" to emptyFormValue,
            )
        )

        val (actualWorkTypes, actualKeyWorkType) = resolveModifiedWorkTypes(
            workTypeLookup,
            initialWorksite,
            modifiedWorksite,
            createdAtB,
        )

        val expectedWorkTypes = listOf(
            testWorkType(WorkTypeType.Sandbagging, createdAtB),
            workTypeChimney,
        )
        assertEquals(expectedWorkTypes, actualWorkTypes)
        assertEquals(workTypeChimney, actualKeyWorkType)
    }
}

private val now = Clock.System.now()

private val createdAtA = now.minus(5.days)
private val createdAtB = createdAtA.plus(14.hours)

internal fun testWorkType(
    workType: WorkTypeType,
    createdAt: Instant = createdAtA,
    id: Long = 0,
    orgClaim: Long? = null,
    status: WorkTypeStatus = WorkTypeStatus.OpenUnassigned,
) = WorkType(
    id = id,
    createdAt = createdAt,
    orgClaim = orgClaim,
    statusLiteral = status.literal,
    workTypeLiteral = workType.name,
)

internal fun testWorksite(
    workTypes: List<WorkType> = emptyList(),
    keyWorkType: WorkType? = null,
    formData: Map<String, WorksiteFormValue>? = emptyMap(),
) = Worksite(
    id = 0,
    address = "address",
    autoContactFrequencyT = "autoContactFrequencyT",
    caseNumber = "caseNumber",
    city = "city",
    county = "county",
    createdAt = now,
    favoriteId = null,
    formData = formData,
    incidentId = 120,
    keyWorkType = keyWorkType,
    latitude = 0.0,
    longitude = 0.0,
    name = "name",
    networkId = 51,
    phone1 = "phone1",
    phone2 = "",
    postalCode = "postalCode",
    reportedBy = null,
    state = "state",
    svi = 0f,
    updatedAt = now,
    what3Words = "what3Words",
    workTypes = workTypes,
)