package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class IncidentRefresher @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private val recentlyRefreshedIncident = AtomicLong(EmptyIncident.id)

    suspend fun pullIncident(id: Long) {
        if (networkMonitor.isNotOnline.first() ||
            recentlyRefreshedIncident.getAndSet(id) == id
        ) {
            return
        }

        try {
            incidentsRepository.pullIncident(id)
            incidentsRepository.pullIncidentOrganizations(id)

            // TODO Query backend for updated locations if incident is recent
        } catch (e: Exception) {
            recentlyRefreshedIncident.set(EmptyIncident.id)
            throw e
        }
    }
}

@Singleton
class LanguageRefresher @Inject constructor(
    private val languageRepository: LanguageTranslationsRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private var lastLoadInstant = AtomicReference(Instant.fromEpochSeconds(0))

    suspend fun pullLanguages() {
        if (networkMonitor.isOnline.first()) {
            val now = Clock.System.now()
            if (now - lastLoadInstant.get() > 6.hours) {
                languageRepository.loadLanguages()
                lastLoadInstant.set(now)
            }
        }
    }
}