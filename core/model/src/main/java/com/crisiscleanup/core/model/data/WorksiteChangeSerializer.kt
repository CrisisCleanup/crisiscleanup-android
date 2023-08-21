package com.crisiscleanup.core.model.data

interface WorksiteChangeSerializer {
    /**
     * Serializes changes to a worksite for applying (on a reference) later
     *
     * Lookups map local ID to network ID where network IDs are specified.
     */
    fun serialize(
        isDataChange: Boolean,
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        flagIdLookup: Map<Long, Long> = emptyMap(),
        noteIdLookup: Map<Long, Long> = emptyMap(),
        workTypeIdLookup: Map<Long, Long> = emptyMap(),
        requestReason: String = "",
        requestWorkTypes: List<String> = emptyList(),
        releaseReason: String = "",
        releaseWorkTypes: List<String> = emptyList(),
    ): Pair<Int, String>
}
