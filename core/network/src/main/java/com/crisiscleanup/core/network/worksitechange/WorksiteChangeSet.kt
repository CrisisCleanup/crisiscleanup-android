package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkWorksitePush
import kotlinx.datetime.Instant

data class WorksiteChangeSet(
    val updatedAtFallback: Instant,
    val worksite: NetworkWorksitePush?,
    val isOrgMember: Boolean?,
    val extraNotes: List<Pair<Long, NetworkNote>> = emptyList(),
    val flagChanges: Pair<List<Pair<Long, NetworkFlag>>, Collection<Long>> =
        Pair(emptyList(), emptyList()),
    val workTypeChanges: List<WorkTypeChange> = emptyList(),
) {
    val hasNonCoreChanges: Boolean
        get() = isOrgMember != null ||
            extraNotes.isNotEmpty() ||
            flagChanges.first.isNotEmpty() ||
            flagChanges.second.isNotEmpty() ||
            workTypeChanges.isNotEmpty()
}
