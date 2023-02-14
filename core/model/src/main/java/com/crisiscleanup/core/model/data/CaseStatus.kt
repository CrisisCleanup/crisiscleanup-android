package com.crisiscleanup.core.model.data

enum class CaseStatus {
    Unknown,
    Unclaimed,
    ClaimedNotStarted,
    InProgress,
    PartiallyCompleted,
    NeedsFollowUp,
    Completed,

    /**
     * Nhw = no help wanted
     * Pc = partially completed
     */
    DoneByOthersNhwPc,

    /**
     * Du = Duplicate or unresponsive
     */
    OutOfScopeDu,
    Incomplete,
}