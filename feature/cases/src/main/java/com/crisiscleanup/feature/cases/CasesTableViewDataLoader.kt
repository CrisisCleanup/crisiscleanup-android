package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class CasesTableViewDataLoader(
    private val worksiteProvider: WorksiteProvider,
    private val worksitesRepository: WorksitesRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val logger: AppLogger,
) {
    private val isLoadingFlagsWorksite = MutableStateFlow(false)
    private val isLoadingWorkTypeWorksite = MutableStateFlow(false)
    val isLoading = combine(
        isLoadingFlagsWorksite,
        isLoadingWorkTypeWorksite
    ) { b0, b1 -> b0 || b1 }

    suspend fun loadWorksiteForAddFlags(worksite: Worksite): Boolean {
        isLoadingFlagsWorksite.value = true
        try {
            var flagsWorksite = worksite
            if (worksite.id > 0 && worksite.networkId > 0) {
                worksiteChangeRepository.trySyncWorksite(worksite.id)
                flagsWorksite = worksitesRepository.getWorksite(worksite.id)
            }

            worksiteProvider.editableWorksite.value = flagsWorksite

            return true
        } finally {
            isLoadingFlagsWorksite.value = false
        }
    }

    suspend fun takeWorkTypeAction(worksite: Worksite) {
        // TODO Pull latest state including work type requests
        // worksitesRepository.pullWorkTypeRequests(networkId)
        //      Do not allow action if there is a conflict between new state and original action
    }
}