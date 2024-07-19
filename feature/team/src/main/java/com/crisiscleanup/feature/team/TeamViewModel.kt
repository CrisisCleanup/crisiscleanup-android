package com.crisiscleanup.feature.team

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
) : ViewModel() {

}
