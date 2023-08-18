package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkFlag
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertEquals

class NetworkFlagChangeTest {
    @Test
    fun noFlags() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())
        val actualEmpties = noFlagsWorksite.getFlagChanges(emptyList(), emptyList(), emptyMap())
        assertEquals(emptyList(), actualEmpties.first)
        assertEquals(emptyList(), actualEmpties.second)
    }

    @Test
    fun noChanges() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())
        val actualNoChange = noFlagsWorksite.getFlagChanges(
            listOf(testFlagSnapshot(3, 32, "reasono")),
            listOf(testFlagSnapshot(3, 32, "reasono")),
            emptyMap(),
        )
        assertEquals(emptyList(), actualNoChange.first)
        assertEquals(emptyList(), actualNoChange.second)
    }

    @Test
    fun addFlags_noOp() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())

        val emptyStart = emptyList<FlagSnapshot>()
        val startOne = listOf(testFlagSnapshot(3, 32, "reasono"))

        // Impossible change to add a networked flag. Do not propagate.
        val actualOne = noFlagsWorksite.getFlagChanges(emptyStart, startOne, emptyMap())
        assertEquals(emptyList(), actualOne.first)
        assertEquals(emptyList(), actualOne.second)
    }

    @Test
    fun addFlags_single() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())

        val emptyStart = emptyList<FlagSnapshot>()
        val startOne = listOf(testFlagSnapshot(3, -1, "reasono"))

        val actualOneLocal = noFlagsWorksite.getFlagChanges(emptyStart, startOne, emptyMap())
        assertEquals(
            listOf(Pair(3L, testNetworkFlag(null, "reasono"))),
            actualOneLocal.first,
        )
        assertEquals(emptyList(), actualOneLocal.second)
    }

    @Test
    fun addFlags_singleLocalDelta() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())

        val startOne = listOf(testFlagSnapshot(3, 34, "reasono"))
        val addedFlags = listOf(
            testFlagSnapshot(3, 34, "reasono"),
            testFlagSnapshot(4, -1, "newer"),
        )

        val actualAdd = noFlagsWorksite.getFlagChanges(startOne, addedFlags, emptyMap())
        assertEquals(listOf(Pair(4L, testNetworkFlag(null, "newer"))), actualAdd.first)
        assertEquals(emptyList(), actualAdd.second)
    }

    @Test
    fun addFlags_localDeltas() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())

        val startOne = listOf(testFlagSnapshot(3, -1, "reasono"))
        val addedFlags = listOf(
            testFlagSnapshot(3, -1, "reasono"),
            testFlagSnapshot(4, -1, "newer"),
        )

        // Assume all unmapped local flags are new.
        // This could happen if prior snapshots were skipped/not pushed successfully.
        val actualAddAll = noFlagsWorksite.getFlagChanges(startOne, addedFlags, emptyMap())
        assertEquals(
            listOf(
                Pair(3L, testNetworkFlag(null, "reasono")),
                Pair(4L, testNetworkFlag(null, "newer")),
            ),
            actualAddAll.first,
        )
        assertEquals(emptyList(), actualAddAll.second)

        // A prior snapshot was successfully pushed and ID mapped.
        val actualAdd = noFlagsWorksite.getFlagChanges(startOne, addedFlags, mapOf(3L to 43L))
        assertEquals(listOf(Pair(4L, testNetworkFlag(null, "newer"))), actualAdd.first)
        assertEquals(emptyList(), actualAdd.second)

        // This could happen when a snapshot was previously synced but
        // never finished normally and ID mappings now exist.
        val actualAddNone = noFlagsWorksite.getFlagChanges(
            startOne,
            addedFlags,
            mapOf(3L to 43L, 4L to 46),
        )
        assertEquals(emptyList(), actualAddNone.first)
        assertEquals(emptyList(), actualAddNone.second)
    }

    @Test
    fun addFlags_multiple() {
        val noFlagsWorksite = testNetworkWorksite(emptyList())

        val emptyStart = emptyList<FlagSnapshot>()
        val addedFlags = listOf(
            testFlagSnapshot(3, -1, "reasono"),
            testFlagSnapshot(4, -1, "newer"),
        )

        val actualMultiple = noFlagsWorksite.getFlagChanges(emptyStart, addedFlags, emptyMap())
        assertEquals(
            listOf(
                Pair(3L, testNetworkFlag(null, "reasono")),
                Pair(4L, testNetworkFlag(null, "newer")),
            ),
            actualMultiple.first,
        )
        assertEquals(emptyList(), actualMultiple.second)
    }

    @Test
    fun addFlags_matchingReason() {
        val flagsWorksite = testNetworkWorksite(listOf(testNetworkFlag(42, "reasono")))

        val emptyStart = emptyList<FlagSnapshot>()
        val startOne = listOf(testFlagSnapshot(3, -1, "reasono"))

        val actualIgnore = flagsWorksite.getFlagChanges(emptyStart, startOne, emptyMap())
        assertEquals(emptyList(), actualIgnore.first)
        assertEquals(emptyList(), actualIgnore.second)

        val addedFlags = listOf(
            testFlagSnapshot(3, -1, "reasono"),
            testFlagSnapshot(4, -1, "newer"),
        )
        val actualAdd = flagsWorksite.getFlagChanges(emptyStart, addedFlags, emptyMap())
        assertEquals(listOf(Pair(4L, testNetworkFlag(null, "newer"))), actualAdd.first)
        assertEquals(emptyList(), actualAdd.second)

        val flagsWorksiteMultiple = testNetworkWorksite(
            listOf(
                testNetworkFlag(42, "reasono"),
                testNetworkFlag(51, "newer"),
            ),
        )
        val actualNone = flagsWorksiteMultiple.getFlagChanges(emptyStart, addedFlags, emptyMap())
        assertEquals(emptyList(), actualNone.first)
        assertEquals(emptyList(), actualNone.second)
    }

    private val deleteFlagsWorksite = testNetworkWorksite(
        listOf(
            testNetworkFlag(42, "reasono"),
            testNetworkFlag(51, "newer"),
            testNetworkFlag(487, "pentag"),
        ),
    )

    @Test
    fun deleteFlags_none() {
        val actualEmpty = deleteFlagsWorksite.getFlagChanges(emptyList(), emptyList(), emptyMap())
        assertEquals(emptyList(), actualEmpty.first)
        assertEquals(emptyList(), actualEmpty.second)

        val actualNoChange = deleteFlagsWorksite.getFlagChanges(
            listOf(testFlagSnapshot(3, -1, "reasono")),
            listOf(testFlagSnapshot(13, -1, "reasono")),
            emptyMap(),
        )
        assertEquals(emptyList(), actualNoChange.first)
        assertEquals(emptyList(), actualNoChange.second)
    }

    @Test
    fun deleteFlags_noneInExisting() {
        val actualNonExisting = deleteFlagsWorksite.getFlagChanges(
            listOf(testFlagSnapshot(3, -1, "horizo")),
            emptyList(),
            emptyMap(),
        )
        assertEquals(emptyList(), actualNonExisting.first)
        assertEquals(emptyList(), actualNonExisting.second)
    }

    @Test
    fun deleteFlags() {
        val actualDeleteOne = deleteFlagsWorksite.getFlagChanges(
            listOf(testFlagSnapshot(3, -1, "newer")),
            emptyList(),
            emptyMap(),
        )
        assertEquals(emptyList(), actualDeleteOne.first)
        assertEquals(listOf(51L), actualDeleteOne.second)

        val actualDeleteTwo = deleteFlagsWorksite.getFlagChanges(
            listOf(
                testFlagSnapshot(3, -1, "newer"),
                testFlagSnapshot(4, -1, "pentag"),
            ),
            emptyList(),
            emptyMap(),
        )
        assertEquals(emptyList(), actualDeleteTwo.first)
        assertEquals(listOf(51L, 487L), actualDeleteTwo.second)
    }

    @Test
    fun complexChanges() {
        val worksite = testNetworkWorksite(
            listOf(
                testNetworkFlag(42, "reasono"),
                testNetworkFlag(51, "newer"),
                testNetworkFlag(487, "pentag"),
                testNetworkFlag(521, "inasmu"),
            ),
        )
        val flagIdLookup = mapOf(
            23L to 531L,
            19L to 25L,
        )
        val startFlags = listOf(
            testFlagSnapshot(53, -1, "change-as-well"),
            testFlagSnapshot(63, -1, "skip-delete-not-in-existing"),
            testFlagSnapshot(73, -1, "reasono"),
            testFlagSnapshot(83, 645, "inasmu"),
        )
        val changeFlags = listOf(
            testFlagSnapshot(33, -1, "pentag"),
            testFlagSnapshot(23, -1, "skip-due-to-lookup"),
            testFlagSnapshot(43, -1, "add-flag"),
            testFlagSnapshot(53, -1, "change-as-well"),
        )

        val actual = worksite.getFlagChanges(startFlags, changeFlags, flagIdLookup)
        assertEquals(
            listOf(
                Pair(43L, testNetworkFlag(null, "add-flag")),
                Pair(53L, testNetworkFlag(null, "change-as-well")),
            ),
            actual.first,
        )
        assertEquals(listOf(42L, 521L), actual.second)
    }
}

private fun testFlagSnapshot(
    localId: Long,
    id: Long,
    reason: String,
    createdAt: Instant = createdAtA,
    isHighPriority: Boolean = false,
) = FlagSnapshot(
    localId,
    FlagSnapshot.Flag(
        id = id,
        action = "",
        createdAt = createdAt,
        isHighPriority = isHighPriority,
        notes = "",
        reasonT = reason,
        reason = reason,
        requestedAction = "",
    ),
)

private fun testNetworkFlag(
    id: Long?,
    reason: String,
    createdAt: Instant = createdAtA,
    isHighPriority: Boolean = false,
    action: String? = null,
    notes: String? = null,
    requestedAction: String? = null,
) = NetworkFlag(
    id = id,
    action = action,
    createdAt = createdAt,
    isHighPriority = isHighPriority,
    notes = notes,
    reasonT = reason,
    requestedAction = requestedAction,
)
