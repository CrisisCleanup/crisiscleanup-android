package com.crisiscleanup.feature.incidentcache

import androidx.lifecycle.ViewModel
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncidentWorksitesCacheViewModel @Inject constructor(
    private val incidentCacheRepository: IncidentCacheRepository,
) : ViewModel()
