package com.crisiscleanup.core.commoncase

import androidx.compose.runtime.State
import com.crisiscleanup.core.model.data.WorkType

interface TransferWorkTypeProvider {
    val isPendingTransfer: State<Boolean>
    val transferType: WorkTypeTransferType
    val workTypes: Map<WorkType, Boolean>
    var reason: String

    val organizationId: Long
    val organizationName: String
    val caseNumber: String

    fun startTransfer(
        organizationId: Long,
        transferType: WorkTypeTransferType,
        workTypes: Map<WorkType, Boolean>,
        organizationName: String = "",
        caseNumber: String = "",
    )

    fun clearPendingTransfer()
}

enum class WorkTypeTransferType {
    None,
    Request,
    Release,
}
