package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksitePush
import kotlinx.datetime.Instant

data class WorksiteChangeSet(
    val updatedAtFallback: Instant,
    val worksite: NetworkWorksitePush?,
    val isOrgMember: Boolean?,
    val extraNotes: List<NetworkWorksiteFull.Note> = emptyList(),
    val flagChanges: Pair<List<NetworkFlag>, Collection<Long>> = Pair(emptyList(), emptyList()),
    val workTypeChanges: Triple<
            List<WorkTypeSnapshot.WorkType>,
            List<WorkTypeChange>,
            Collection<Long>,
            > = Triple(
        emptyList(),
        emptyList(),
        emptyList()
    ),
)