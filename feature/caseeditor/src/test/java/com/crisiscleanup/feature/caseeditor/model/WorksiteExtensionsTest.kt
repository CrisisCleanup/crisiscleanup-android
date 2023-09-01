package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFlagType
import com.crisiscleanup.feature.caseeditor.util.flagsEqualizeCreatedAt
import com.crisiscleanup.feature.caseeditor.util.testWorksite
import org.junit.Test
import kotlin.test.assertEquals

class WorksiteExtensionsTest {
    @Test
    fun testCopyModifiedFlag_noChange() {
        val flagsWorksite = testWorksite(
            listOf(WorksiteFlag.flag(WorksiteFlagType.Duplicate)),
        )

        val noChangeCopyMissing = flagsWorksite.copyModifiedFlags(
            false,
            { false },
            WorksiteFlag::highPriority,
        )
        assertEquals(flagsWorksite.flags, noChangeCopyMissing)

        val noChangeCopyExists = flagsWorksite.copyModifiedFlags(
            true,
            { true },
            WorksiteFlag::highPriority,
        )
        assertEquals(flagsWorksite.flags, noChangeCopyExists)
    }

    @Test
    fun testCopyModifiedFlag_changes() {
        val flagsWorksite = testWorksite()

        // Removing flag not in flags does not fail
        val removeNonExisting = flagsWorksite.copyModifiedFlags(
            false,
            { true },
            WorksiteFlag::highPriority,
        )
        assertEquals(flagsWorksite.flags, removeNonExisting)

        // Add a few flags
        var addFlags = flagsWorksite
            .copyModifiedFlags(
                true,
                { false },
                WorksiteFlag::highPriority,
            )
        addFlags = flagsWorksite.copy(flags = addFlags)
            .copyModifiedFlags(
                true,
                { false },
                WorksiteFlag::wrongLocation,
            )
        addFlags = flagsWorksite.copy(flags = addFlags)
            .copyModifiedFlags(
                true,
                { false },
                {
                    WorksiteFlag.flag(
                        WorksiteFlagType.MarkForDeletion,
                        "delete-notes",
                        "delete-action",
                    )
                },
            )

        val expectedAddFlags = listOf(
            WorksiteFlag.highPriority(),
            WorksiteFlag.wrongLocation(),
            WorksiteFlag.flag(
                WorksiteFlagType.MarkForDeletion,
                "delete-notes",
                "delete-action",
            ),
        )

        assertEquals(
            expectedAddFlags.flagsEqualizeCreatedAt(),
            addFlags!!.flagsEqualizeCreatedAt(),
        )

        val addFlagsWorksite = flagsWorksite.copy(flags = addFlags)

        // Remove flag(s)
        var removeFlags = addFlagsWorksite
            .copyModifiedFlags(
                false,
                { it.isHighPriority || it.isHighPriorityFlag },
                { WorksiteFlag.highPriority() },
            )
        removeFlags = addFlagsWorksite.copy(flags = removeFlags)
            .copyModifiedFlags(
                false,
                { it.flagType == WorksiteFlagType.UpsetClient },
                { WorksiteFlag.flag(WorksiteFlagType.UpsetClient) },
            )
        removeFlags = addFlagsWorksite.copy(flags = removeFlags)
            .copyModifiedFlags(
                false,
                { it.flagType == WorksiteFlagType.MarkForDeletion },
                { WorksiteFlag.flag(WorksiteFlagType.MarkForDeletion) },
            )

        val expectedRemoveFlags = listOf(
            WorksiteFlag.wrongLocation(),
        )

        assertEquals(
            expectedRemoveFlags.flagsEqualizeCreatedAt(),
            removeFlags!!.flagsEqualizeCreatedAt(),
        )
    }
}
