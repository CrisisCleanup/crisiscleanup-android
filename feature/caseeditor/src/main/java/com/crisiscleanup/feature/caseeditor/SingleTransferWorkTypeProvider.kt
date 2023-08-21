package com.crisiscleanup.feature.caseeditor

import androidx.compose.runtime.mutableStateOf
import com.crisiscleanup.core.commoncase.TransferWorkTypeProvider
import com.crisiscleanup.core.commoncase.WorkTypeTransferType
import com.crisiscleanup.core.model.data.WorkType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SingleTransferWorkTypeProvider @Inject constructor() : TransferWorkTypeProvider {
    override val isPendingTransfer = mutableStateOf(false)

    override var transferType = WorkTypeTransferType.None
        private set

    override var workTypes = emptyMap<WorkType, Boolean>()
        private set

    override var reason = ""

    override var organizationId = 0L
        private set
    override var organizationName = ""
        private set
    override var caseNumber = ""
        private set

    override fun startTransfer(
        organizationId: Long,
        transferType: WorkTypeTransferType,
        workTypes: Map<WorkType, Boolean>,
        organizationName: String,
        caseNumber: String,
    ) {
        if (transferType != WorkTypeTransferType.None &&
            workTypes.isNotEmpty()
        ) {
            this.organizationId = organizationId
            reason = ""
            this.transferType = transferType
            this.workTypes = workTypes
            isPendingTransfer.value = true

            this.organizationName = organizationName
            this.caseNumber = caseNumber
        } else {
            this.transferType = WorkTypeTransferType.None
        }
    }

    override fun clearPendingTransfer() {
        isPendingTransfer.value = false
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WorkTypeTransferModule {
    @Binds
    @Singleton
    fun bindsWorkTypeTransferProvider(
        provider: SingleTransferWorkTypeProvider,
    ): TransferWorkTypeProvider
}
