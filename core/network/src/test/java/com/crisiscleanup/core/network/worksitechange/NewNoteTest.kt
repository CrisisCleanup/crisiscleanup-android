package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkNote
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class NewNoteTest {
    @Test
    fun noNewNotes() {
        val snapshot = testWorksiteSnapshot(
            notes = listOf(
                testNoteSnapshot("", -1, -1),
                testNoteSnapshot("not-blank", 53, -1),
                testNoteSnapshot("not-blank", -1, 46),
            ),
        )
        val actual = snapshot.getNewNetworkNotes(mapOf(53L to 35))
        assertEquals(emptyList(), actual)
    }

    @Test
    fun newNotes() {
        val snapshot = testWorksiteSnapshot(
            notes = listOf(
                testNoteSnapshot("not-blank-a", 44, -1, createdAt = createdAtA),
                testNoteSnapshot("not-blank-b", 53, -1),
            ),
        )
        val actual = snapshot.getNewNetworkNotes(mapOf(53L to 35))
        val expected = listOf(
            Pair(44L, NetworkNote(null, createdAtA, false, "not-blank-a")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun filterDuplicateNotes() {
        val now = Clock.System.now()
        val nineDaysAgo = now.minus(9.days)
        val eightDaysAgo = now.minus(8.days)
        val sevenDaysAgo = now.minus(7.days)
        val twoDaysAgo = now.minus(2.days)
        val oneDayAgo = now.minus(1.days)
        val worksite = testNetworkWorksite(
            notes = listOf(
                NetworkNote(11, nineDaysAgo, false, "note-a"),
                NetworkNote(12, eightDaysAgo, true, "note-b"),
                NetworkNote(13, sevenDaysAgo, false, "note-c"),
                NetworkNote(14, twoDaysAgo, true, "note-d"),
                NetworkNote(15, oneDayAgo, false, "note-e"),
            ),
        )

        val actual = worksite.filterDuplicateNotes(
            listOf(
                Pair(1L, NetworkNote(11, now, false, "note-a")),
                Pair(2L, NetworkNote(-1, eightDaysAgo.plus(1.minutes), false, "note-b")),
                Pair(3L, NetworkNote(-1, sevenDaysAgo.plus(11.hours), false, " note-c")),
                Pair(4L, NetworkNote(-1, sevenDaysAgo, true, "new-note-a")),
                Pair(5L, NetworkNote(-1, now, false, "note-d")),
                Pair(6L, NetworkNote(-1, oneDayAgo.minus(11.hours), true, "note-e")),
            ),
        )

        val expected = listOf(
            Pair(1L, NetworkNote(11, now, false, "note-a")),
            Pair(4L, NetworkNote(-1, sevenDaysAgo, true, "new-note-a")),
            Pair(5L, NetworkNote(-1, now, false, "note-d")),
        )
        assertEquals(actual, expected)
    }
}

internal val emptyCoreSnapshot = CoreSnapshot(
    id = 1,
    address = "",
    autoContactFrequencyT = "",
    caseNumber = "",
    city = "",
    county = "",
    createdAt = Clock.System.now(),
    email = "",
    favoriteId = 0,
    formData = emptyMap(),
    incidentId = 1,
    keyWorkTypeId = 1,
    latitude = 0.0,
    longitude = 0.0,
    name = "",
    networkId = 1,
    phone1 = "",
    phone2 = "",
    plusCode = "",
    postalCode = "",
    reportedBy = 1,
    state = "",
    svi = 0f,
    updatedAt = null,
    what3Words = "",
    isAssignedToOrgMember = false,
)

internal fun testWorksiteSnapshot(
    core: CoreSnapshot = emptyCoreSnapshot,
    flags: List<FlagSnapshot> = emptyList(),
    notes: List<NoteSnapshot> = emptyList(),
    workTypes: List<WorkTypeSnapshot> = emptyList(),
) = WorksiteSnapshot(
    core = core,
    flags = flags,
    notes = notes,
    workTypes = workTypes,
)

private fun testNoteSnapshot(
    note: String,
    localId: Long = -1,
    networkId: Long = -1,
    createdAt: Instant = Clock.System.now(),
) = NoteSnapshot(
    localId = localId,
    note = NoteSnapshot.Note(
        id = networkId,
        createdAt = createdAt,
        isSurvivor = false,
        note = note,
    ),
)
