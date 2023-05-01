package com.crisiscleanup.core.model.data

// TODO Can this be consolidated with WorkTypeStatus? Or are these distinct states?
enum class CaseStatus {
    Unknown,
    Unclaimed,
    ClaimedNotStarted,
    InProgress,
    PartiallyCompleted,
    NeedsFollowUp,
    Completed,

    // TODO Review colors (and names) on web. There are marker colors and status colors...
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