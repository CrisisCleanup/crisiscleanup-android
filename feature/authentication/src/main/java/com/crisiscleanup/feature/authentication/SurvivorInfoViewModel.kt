package com.crisiscleanup.feature.authentication

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.CmsResultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SurvivorInfoViewModel @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Auth) private val logger: AppLogger,
) : ViewModel() {

    val isLoading = MutableStateFlow(false)
    val survivorInfoData = MutableStateFlow(listOf<CmsResultItem>())

    init {
        viewModelScope.launch(ioDispatcher) {
            isLoading.value = true
            survivorInfoData.value = networkDataSource.getCms(
                listOf(
                    "survivor-info",
                ),
            )
            isLoading.value = false
            Log.d("SURVIVOR FROM VM", survivorInfoData.toString())
        }
    }

}

