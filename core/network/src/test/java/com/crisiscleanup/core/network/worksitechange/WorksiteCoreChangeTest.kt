package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import com.crisiscleanup.core.network.model.NetworkEvent
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkType
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksitePush
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorksiteCoreChangeTest {
    private val minDefinedWorksite = NetworkWorksiteFull(
        id = 42,
        address = "address-min",
        autoContactFrequencyT = "auto-frequency-min",
        caseNumber = "case-min",
        city = "city-min",
        county = "county-min",
        email = null,
        events = emptyList(),
        favorite = NetworkType(38, "", createdAtA),
        files = emptyList(),
        flags = emptyList(),
        formData = emptyList(),
        incident = 486,
        keyWorkType = NetworkWorkType(
            454,
            createdAtA,
            status = "status-min",
            workType = "work-type-min",
        ),
        location = NetworkWorksiteFull.Location("Point", listOf(5141.3, 51341.4)),
        name = "name-min",
        notes = emptyList(),
        phone1 = "phone-min",
        phone2 = null,
        plusCode = null,
        postalCode = "postal-min",
        reportedBy = null,
        state = "state-min",
        svi = null,
        // times = emptyList(),
        updatedAt = updatedAtA,
        what3words = null,
        workTypes = listOf(
            NetworkWorkType(
                454,
                createdAtA,
                status = "status-min",
                workType = "work-type-min",
            )
        ),
    )
    private val fullyDefinedWorksite = NetworkWorksiteFull(
        id = 67,
        address = "address",
        autoContactFrequencyT = "auto-frequency",
        caseNumber = "case",
        city = "city",
        county = "county",
        email = "email",
        events = listOf(
            NetworkEvent(
                643,
                emptyMap(),
                createdAtA,
                853,
                "key",
                9684,
                "patient",
                NetworkEvent.Description("key", "description", "name")
            ),
        ),
        favorite = NetworkType(38, "favorite", createdAtA),
        files = emptyList(),
        flags = listOf(
            NetworkFlag(53853, "action", createdAtA, false, "notes", "reason", null)
        ),
        formData = listOf(
            KeyDynamicValuePair("key", DynamicValue("dynamic-value"))
        ),
        incident = 875,
        keyWorkType = NetworkWorkType(
            5498,
            createdAtB,
            status = "status-b",
            workType = "work-type-b",
        ),
        location = NetworkWorksiteFull.Location("Point", listOf(-534.53513, 534.1353)),
        name = "name",
        notes = listOf(
            NetworkNote(4845, createdAtA, false, "note"),
        ),
        phone1 = "phone",
        phone2 = "phone-2",
        plusCode = "plus-code",
        postalCode = "postal-code",
        reportedBy = 683,
        state = "state",
        svi = 0.5f,
        // times = emptyList(),
        updatedAt = updatedAtA,
        what3words = "what-three-words",
        workTypes = listOf(
            NetworkWorkType(
                95,
                createdAtA,
                status = "status-a",
                workType = "work-type-a",
            ),
            NetworkWorkType(
                5498,
                createdAtB,
                status = "status-b",
                workType = "work-type-b",
            ),
        ),
    )

    private val baseSnapshot = testCoreSnapshot(
        address = "address-snapshot",
        autoContactFrequencyT = "frequency-snapshot",
        caseNumber = "case-snapshot",
        city = "city-snapshot",
        county = "county-snapshot",
        createdAt = createdAtA,
        email = "email-snapshot",
        incidentId = 75,
        latitude = -85.3523,
        longitude = -23.38,
        name = "name-snapshot",
        phone1 = "phone-snapshot",
        postalCode = "postal-snapshot",
        reportedBy = 83,
        state = "state-snapshot",
        updatedAt = updatedAtA,
    )

    @Test
    fun noChanges() {
        val actualMin = minDefinedWorksite.getCoreChange(
            baseSnapshot,
            baseSnapshot,
            emptyList(),
            null,
            baseSnapshot.updatedAt!!,
        )
        assertNull(actualMin)

        val actualFull = fullyDefinedWorksite.getCoreChange(
            baseSnapshot,
            baseSnapshot.copy(updatedAt = updatedAtB),
            emptyList(),
            null,
            baseSnapshot.updatedAt!!,
        )
        assertNull(actualFull)
    }

    @Test
    fun singleChange() {
        val changeSnapshot = baseSnapshot.copy(address = "address-updated")

        val actual = fullyDefinedWorksite.getCoreChange(
            baseSnapshot,
            changeSnapshot,
            emptyList(),
            null,
            changeSnapshot.updatedAt!!,
        )

        val expected = NetworkWorksitePush(
            id = fullyDefinedWorksite.id,
            address = changeSnapshot.address,
            autoContactFrequencyT = fullyDefinedWorksite.autoContactFrequencyT,
            caseNumber = fullyDefinedWorksite.caseNumber,
            city = fullyDefinedWorksite.city,
            county = fullyDefinedWorksite.county,
            email = fullyDefinedWorksite.email,
            favorite = fullyDefinedWorksite.favorite,
            formData = emptyList(),
            incident = fullyDefinedWorksite.incident,
            keyWorkType = null,
            location = fullyDefinedWorksite.location,
            name = fullyDefinedWorksite.name,
            phone1 = fullyDefinedWorksite.phone1,
            phone2 = fullyDefinedWorksite.phone2,
            plusCode = fullyDefinedWorksite.plusCode,
            postalCode = fullyDefinedWorksite.postalCode,
            reportedBy = fullyDefinedWorksite.reportedBy,
            state = fullyDefinedWorksite.state,
            svi = fullyDefinedWorksite.svi,
            updatedAt = fullyDefinedWorksite.updatedAt,
            what3words = fullyDefinedWorksite.what3words,
            workTypes = emptyList(),

            skipDuplicateCheck = true,
            sendSms = null,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun minChanges() {
        val changeSnapshot = testCoreSnapshot(
            address = "address-change",
            autoContactFrequencyT = "frequency-change",
            caseNumber = "case-change",
            city = "city-change",
            county = "county-change",
            createdAt = createdAtB,
            email = "email-change",
            incidentId = 64,
            latitude = 64.84,
            longitude = 56.458,
            name = "name-change",
            phone1 = "phone-change",
            postalCode = "postal-change",
            reportedBy = 151,
            state = "state-change",
            updatedAt = updatedAtB,
        )
        val actual = fullyDefinedWorksite.getCoreChange(
            baseSnapshot,
            changeSnapshot,
            emptyList(),
            null,
            changeSnapshot.updatedAt!!,
        )

        val expected = NetworkWorksitePush(
            id = fullyDefinedWorksite.id,
            address = changeSnapshot.address,
            autoContactFrequencyT = changeSnapshot.autoContactFrequencyT,
            caseNumber = fullyDefinedWorksite.caseNumber,
            city = changeSnapshot.city,
            county = changeSnapshot.county,
            email = changeSnapshot.email,
            favorite = fullyDefinedWorksite.favorite,
            formData = emptyList(),
            incident = changeSnapshot.incidentId,
            keyWorkType = null,
            location = NetworkWorksiteFull.Location(
                "Point",
                listOf(changeSnapshot.longitude, changeSnapshot.latitude)
            ),
            name = changeSnapshot.name,
            phone1 = changeSnapshot.phone1,
            phone2 = fullyDefinedWorksite.phone2,
            plusCode = fullyDefinedWorksite.plusCode,
            postalCode = changeSnapshot.postalCode,
            reportedBy = fullyDefinedWorksite.reportedBy,
            state = changeSnapshot.state,
            svi = fullyDefinedWorksite.svi,
            updatedAt = changeSnapshot.updatedAt!!,
            what3words = fullyDefinedWorksite.what3words,
            workTypes = emptyList(),

            skipDuplicateCheck = true,
            sendSms = null,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun fullChanges() {
        val changeSnapshot = testCoreSnapshot(
            address = "address-change",
            autoContactFrequencyT = "frequency-change",
            caseNumber = "case-change",
            city = "city-change",
            county = "county-change",
            createdAt = createdAtB,
            email = "email-change",
            favoriteId = 523,
            formData = mapOf("a" to DynamicValue("a-value")),
            incidentId = 75,
            keyWorkTypeId = 835,
            latitude = 64.84,
            longitude = 56.458,
            name = "name-change",
            networkId = 85014,
            phone1 = "phone-change",
            phone2 = "phone-2-change",
            plusCode = "plus-code-change",
            postalCode = "postal-change",
            reportedBy = 151,
            state = "state-change",
            svi = 0.3f,
            updatedAt = updatedAtB,
            what3Words = "what-3-words-change",
            isAssignedToOrgMember = true,
        )
        val actual = fullyDefinedWorksite.getCoreChange(
            baseSnapshot,
            changeSnapshot,
            emptyList(),
            null,
            changeSnapshot.updatedAt!!,
        )

        val expected = NetworkWorksitePush(
            id = fullyDefinedWorksite.id,
            address = changeSnapshot.address,
            autoContactFrequencyT = changeSnapshot.autoContactFrequencyT,
            caseNumber = fullyDefinedWorksite.caseNumber,
            city = changeSnapshot.city,
            county = changeSnapshot.county,
            email = changeSnapshot.email,
            favorite = fullyDefinedWorksite.favorite,
            formData = emptyList(),
            incident = fullyDefinedWorksite.incident,
            keyWorkType = null,
            location = NetworkWorksiteFull.Location(
                "Point",
                listOf(changeSnapshot.longitude, changeSnapshot.latitude)
            ),
            name = changeSnapshot.name,
            phone1 = changeSnapshot.phone1,
            phone2 = changeSnapshot.phone2,
            plusCode = changeSnapshot.plusCode,
            postalCode = changeSnapshot.postalCode,
            reportedBy = fullyDefinedWorksite.reportedBy,
            state = changeSnapshot.state,
            svi = fullyDefinedWorksite.svi,
            updatedAt = changeSnapshot.updatedAt!!,
            what3words = changeSnapshot.what3Words,
            workTypes = emptyList(),

            skipDuplicateCheck = true,
            sendSms = null,
        )
        assertEquals(expected, actual)
    }
}