package com.crisiscleanup.core.commoncase

import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CaseFlagsNavigationState(
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val worksitesRepository: WorksitesRepository,
    private val worksiteProvider: WorksiteProvider,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher,
) {
    private val _isLoadingFlagsWorksite = MutableStateFlow(false)
    val isLoadingFlagsWorksite: StateFlow<Boolean> = _isLoadingFlagsWorksite

    private val _openWorksiteAddFlagCounter = MutableStateFlow(0)
    val openWorksiteAddFlagCounter: StateFlow<Int> = _openWorksiteAddFlagCounter
    private val openWorksiteAddFlag = AtomicBoolean(false)

    fun onOpenCaseFlags(worksite: Worksite) {
        coroutineScope.launch(coroutineDispatcher) {
            if (loadWorksiteForAddFlags(worksite)) {
                openWorksiteAddFlag.set(true)
                _openWorksiteAddFlagCounter.value++
            }
        }
    }

    private suspend fun loadWorksiteForAddFlags(worksite: Worksite): Boolean {
        _isLoadingFlagsWorksite.value = true
        try {
            var flagsWorksite = worksite
            if (worksite.id > 0 && worksite.networkId > 0) {
                worksiteChangeRepository.trySyncWorksite(worksite.id)
                flagsWorksite = worksitesRepository.getWorksite(worksite.id)
            }

            worksiteProvider.editableWorksite.value = flagsWorksite

            return true
        } finally {
            _isLoadingFlagsWorksite.value = false
        }
    }

    fun takeOpenWorksiteAddFlag() = openWorksiteAddFlag.getAndSet(false)
}
