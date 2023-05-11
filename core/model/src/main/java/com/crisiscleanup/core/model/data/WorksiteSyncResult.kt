package com.crisiscleanup.core.model.data

data class WorksiteSyncResult(
    val changeResults: Collection<ChangeResult>,
    val changeIds: ChangeIds,
) {
    data class ChangeResult(
        // Local ID
        val id: Long,
        val isSuccessful: Boolean,
        val isPartiallySuccessful: Boolean,
        val isFail: Boolean,
    )

    data class ChangeIds(
        val networkWorksiteId: Long,
        val flagIdMap: Map<Long, Long>,
        val noteIdMap: Map<Long, Long>,
        val workTypeIdMap: Map<Long, Long>,
        val workTypeKeyMap: Map<String, Long>,
    )

    private fun <T, R> summarizeChanges(changeMap: Map<T, R>, postText: String): String? =
        if (changeMap.isEmpty()) null
        else "${changeMap.size} $postText"

    fun getSummary(totalChangeCount: Int): String {
        var successCount = 0
        var partialSuccessCount = 0
        var failCount = 0
        changeResults.forEach {
            if (it.isSuccessful) successCount++
            else if (it.isPartiallySuccessful) partialSuccessCount++
            else if (it.isFail) failCount++
        }
        val outcomeSummary = if (totalChangeCount > 1) {
            listOf(
                "$totalChangeCount changes",
                "  $successCount success",
                "  $partialSuccessCount partial",
                "  $failCount fail",
            ).joinToString("\n")
        } else {
            "1 change: " + (
                    if (successCount > 0) "success"
                    else if (partialSuccessCount > 0) "partial"
                    else if (failCount > 0) "fail"
                    else ""
                    )
        }
        val changeTypeSummary = with(changeIds) {
            listOf(
                summarizeChanges(flagIdMap, "flags"),
                summarizeChanges(noteIdMap, "notes"),
                summarizeChanges(workTypeIdMap, "work type IDs"),
                summarizeChanges(workTypeKeyMap, "work type keys"),
            )
                .filter { it?.isNotBlank() == true }
                .joinToString("\n")
        }

        return listOf(
            "Network ID: ${changeIds.networkWorksiteId}",
            outcomeSummary,
            changeTypeSummary
        )
            .joinToString("\n")
    }
}
