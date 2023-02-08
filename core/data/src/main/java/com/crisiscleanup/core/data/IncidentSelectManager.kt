package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentSelector {
    val incidentId: StateFlow<Long>

    val incident: StateFlow<Incident>

    suspend fun setIncident(incident: Incident)
}

@Singleton
class IncidentSelectManager @Inject constructor(
    @ApplicationScope coroutineScope: CoroutineScope,
) : IncidentSelector {
    override var incident = MutableStateFlow(EmptyIncident)
        private set

    override val incidentId = incident.mapLatest { it.id }
        .stateIn(
            scope = coroutineScope,
            initialValue = EmptyIncident.id,
            started = SharingStarted.WhileSubscribed(1_000),
        )

    override suspend fun setIncident(incident: Incident) {
        this.incident.value = incident
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface IncidentSelectModule {
    @Singleton
    @Binds
    fun bindsIncidentSelector(selector: IncidentSelectManager): IncidentSelector
}