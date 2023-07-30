package com.crisiscleanup.core.model.data

fun Worksite.getClaimStatus(affiliateIds: Set<Long>): TableWorksiteClaimStatus {
    val workTypeCount = workTypes.size
    val claimedBy = workTypes.mapNotNull(WorkType::orgClaim)
    val unclaimedCount = workTypeCount - claimedBy.size
    var claimStatus = TableWorksiteClaimStatus.HasUnclaimed
    if (unclaimedCount == 0) {
        val claimedByMyOrgCount = claimedBy.filter { affiliateIds.contains(it) }.size
        claimStatus =
            if (claimedByMyOrgCount > 0) TableWorksiteClaimStatus.ClaimedByMyOrg
            else TableWorksiteClaimStatus.ClaimedByOthers

        // TODO Test
        if (claimStatus == TableWorksiteClaimStatus.ClaimedByOthers &&
            workTypeRequests.filter(WorkTypeRequest::hasNoResponse).size == workTypeCount
        ) {
            claimStatus = TableWorksiteClaimStatus.Requested
        }
    }
    return claimStatus
}

data class TableDataWorksite(
    val worksite: Worksite,
    val claimStatus: TableWorksiteClaimStatus,
)

enum class TableWorksiteClaimStatus {
    HasUnclaimed,

    ClaimedByMyOrg,

    /**
     * Claimed by an unaffiliated org
     */
    ClaimedByOthers,

    /**
     * All work types have been requested
     */
    Requested,
}

enum class TableWorksiteClaimAction {
    Claim,
    Unclaim,
    Request,
    Release,
}