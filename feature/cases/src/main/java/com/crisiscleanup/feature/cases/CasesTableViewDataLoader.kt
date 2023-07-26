package com.crisiscleanup.feature.cases

import com.crisiscleanup.core.commoncase.WorksiteProvider
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class CasesTableViewDataLoader(
    worksiteProvider: WorksiteProvider,
    worksitesRepository: WorksitesRepository,
    worksiteChangeRepository: WorksiteChangeRepository,
) {
    private val isLoadingFlagsWorksite = MutableStateFlow(false)
    private val isLoadingWorkTypeWorksite = MutableStateFlow(false)
    val isLoading = combine(
        isLoadingFlagsWorksite,
        isLoadingWorkTypeWorksite
    ) { b0, b1 -> b0 || b1 }

    suspend fun loadWorksiteForAddFlags(worksiteId: Long): Boolean {
        isLoadingFlagsWorksite.value = true
        try {
            // TODO
            delay(1000)
        } finally {
            isLoadingFlagsWorksite.value = false
        }
        return false
    }
}