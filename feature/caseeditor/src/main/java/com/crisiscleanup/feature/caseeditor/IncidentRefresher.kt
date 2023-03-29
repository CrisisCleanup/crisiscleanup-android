package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidentRefresher @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private val recentlyRefreshedIncident = AtomicLong(EmptyIncident.id)

    suspend fun refreshIncident(id: Long) {
        if (networkMonitor.isNotOnline.first() ||
            recentlyRefreshedIncident.get() == id
        ) {
            return
        }
        recentlyRefreshedIncident.set(id)

        try {
            incidentsRepository.pullIncident(id)

            // TODO Query backend for updated locations if incident is recent
        } catch (e: Exception) {
            recentlyRefreshedIncident.set(EmptyIncident.id)
            throw e
        }
    }
}