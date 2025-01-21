package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.epochZero
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.UsersRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.atomic.AtomicLong
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
    private var lastRefresh = Instant.epochZero

    suspend fun pullLanguages() {
        val now = Clock.System.now()
        if (networkMonitor.isOnline.first() &&
            now - lastRefresh > 6.hours
        ) {
            lastRefresh = now

            languageRepository.loadLanguages()
        }
    }
}

@Singleton
class OrganizationRefresher @Inject constructor(
    private val accountDataRefresher: AccountDataRefresher,
    private val usersRepository: UsersRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private var incidentIdPull = EmptyIncident.id
    private var lastOrganizationRefresh = Instant.epochZero
    private var lastOrganizationAffiliatesRefresh = Instant.epochZero
    private var lastOrganizationUsersRefresh = Instant.epochZero

    suspend fun pullOrganization(incidentId: Long) {
        val now = Clock.System.now()
        if (networkMonitor.isOnline.first() &&
            (
                incidentIdPull != incidentId ||
                    now - lastOrganizationRefresh > 1.hours
                )
        ) {
            incidentIdPull = incidentId
            lastOrganizationRefresh = now

            accountDataRefresher.updateMyOrganization(true)
        }
    }

    suspend fun pullOrganizationAndAffiliates(force: Boolean = false) {
        val now = Clock.System.now()
        if (networkMonitor.isOnline.first() &&
            (
                force ||
                    now - lastOrganizationAffiliatesRefresh > 1.hours
                )
        ) {
            lastOrganizationAffiliatesRefresh = now

            accountDataRefresher.updateOrganizationAndAffiliates()
        }
    }

    suspend fun pullOrganizationUsers(force: Boolean = false) {
        val now = Clock.System.now()
        if (networkMonitor.isOnline.first() &&
            (
                force ||
                    now - lastOrganizationUsersRefresh > 3.hours
                )
        ) {
            lastOrganizationUsersRefresh = now

            if (!usersRepository.queryOrganizationTeamMembers()) {
                lastOrganizationUsersRefresh = Instant.epochZero
            }
        }
    }
}

@Singleton
class UserRoleRefresher @Inject constructor(
    private val usersRepository: UsersRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private var lastRefresh = Instant.epochZero

    suspend fun pullRoles() {
        val now = Clock.System.now()
        if (networkMonitor.isOnline.first() &&
            now - lastRefresh > 6.hours
        ) {
            lastRefresh = now

            usersRepository.loadUserRoles()
        }
    }
}
