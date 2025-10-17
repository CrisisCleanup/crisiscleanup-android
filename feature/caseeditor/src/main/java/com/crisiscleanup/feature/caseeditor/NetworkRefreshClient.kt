package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

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

@Singleton
class OrganizationRefresher @Inject constructor(
    private val accountDataRefresher: AccountDataRefresher,
) {
    private var incidentIdPull = EmptyIncident.id
    private var pullTime = Instant.fromEpochSeconds(0)

    suspend fun pullOrganization(incidentId: Long) {
        if (incidentIdPull == incidentId &&
            Clock.System.now() - pullTime < 1.hours
        ) {
            return
        }
        incidentIdPull = EmptyIncident.id
        pullTime = Clock.System.now()

        accountDataRefresher.updateMyOrganization(true)
    }
}
