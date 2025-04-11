package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.data.model.filter
import com.crisiscleanup.core.database.dao.RecentWorksiteDao
import com.crisiscleanup.core.database.dao.WorkTypeTransferRequestDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.model.PopulatedRecentWorksite
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asSummary
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.IncidentIdWorksiteCount
import com.crisiscleanup.core.model.data.OrganizationLocationAreaBounds
import com.crisiscleanup.core.model.data.TableDataWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.core.model.data.getClaimStatus
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstWorksitesRepository @Inject constructor(
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    accountDataRepository: AccountDataRepository,
    private val languageTranslationsRepository: LanguageTranslationsRepository,
    private val recentWorksiteDao: RecentWorksiteDao,
    private val dataSource: CrisisCleanupNetworkDataSource,
    private val writeApi: CrisisCleanupWriteApi,
    private val workTypeTransferRequestDaoPlus: WorkTypeTransferRequestDaoPlus,
    organizationsRepository: OfflineFirstOrganizationsRepository,
    private val filterRepository: CasesFilterRepository,
    private val locationProvider: LocationProvider,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
    @ApplicationScope externalScope: CoroutineScope,
) : WorksitesRepository {
    override val isDeterminingWorksitesCount = MutableStateFlow(false)

    private val orgId = accountDataRepository.accountData.map { it.org.id }

    private val organizationLocationAreaBounds = orgId
        .filter { it > 0 }
        .flatMapLatest {
            organizationsRepository.streamPrimarySecondaryAreas(it)
        }
        .stateIn(
            scope = externalScope,
            initialValue = OrganizationLocationAreaBounds(),
            started = SharingStarted.WhileSubscribed(),
        )

    @OptIn(FlowPreview::class)
    override fun streamIncidentWorksitesCount(incidentIdStream: Flow<Long>) = combine(
        incidentIdStream.flatMapLatest { id ->
            worksiteDao.streamWorksitesCount(id).map { count ->
                Pair(id, count)
            }
        },
        filterRepository.casesFiltersLocation,
        organizationLocationAreaBounds,
        ::Triple,
    )
        .map { (idCount, filtersLocation, areaBounds) ->
            Pair(
                idCount,
                Pair(filtersLocation, areaBounds),
            )
        }
        .debounce(timeoutMillis = 150)
        .distinctUntilChanged()
        .mapLatest { streams ->
            val (id, totalCount) = streams.first
            val (filtersLocation, areaBounds) = streams.second

            val filters = filtersLocation.first
            if (!filters.isDefault) {
                isDeterminingWorksitesCount.value = true
                try {
                    organizationAffiliates.first()
                    return@mapLatest worksiteDaoPlus.getWorksitesCount(
                        id,
                        totalCount,
                        filters,
                        organizationAffiliates.value,
                        locationProvider.getLocation(),
                        areaBounds,
                    )
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    isDeterminingWorksitesCount.value = false
                }
            }

            isDeterminingWorksitesCount.value = false
            IncidentIdWorksiteCount(id, totalCount, totalCount)
        }

    private val organizationAffiliates = orgId.map {
        organizationsRepository.getOrganizationAffiliateIds(it, true).toSet()
    }
        .stateIn(
            scope = externalScope,
            initialValue = emptySet(),
            started = SharingStarted.WhileSubscribed(),
        )

    override fun streamLocalWorksite(worksiteId: Long) =
        worksiteDao.streamLocalWorksite(worksiteId).map {
            it?.asExternalModel(
                orgId.first(),
                languageTranslationsRepository,
            )
        }

    override suspend fun getWorksite(worksiteId: Long) =
        worksiteDao.getWorksite(worksiteId).asExternalModel(
            orgId.first(),
            languageTranslationsRepository,
        )
            .worksite

    override fun streamRecentWorksites(incidentId: Long) =
        recentWorksiteDao.streamRecentWorksites(incidentId)
            .map { it.map(PopulatedRecentWorksite::asSummary) }

    override fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int,
        coordinates: Pair<Double, Double>?,
    ) = worksiteDao.getWorksitesMapVisual(
        incidentId,
        latitudeSouth,
        latitudeNorth,
        longitudeWest,
        longitudeEast,
        limit,
        offset,
    )
        .filter(
            filterRepository.casesFilters,
            // TODO Ensure these have loaded prior to calling this method
            organizationAffiliates.value,
            organizationLocationAreaBounds.value,
            coordinates,
        )

    override fun getWorksitesCount(incidentId: Long) = worksiteDao.getWorksitesCount(incidentId)

    override fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ) = worksiteDaoPlus.getWorksitesCount(
        incidentId,
        latitudeSouth,
        latitudeNorth,
        longitudeLeft,
        longitudeRight,
    )

    override fun getLocalId(networkWorksiteId: Long) = worksiteDao.getWorksiteId(networkWorksiteId)

    override suspend fun syncNetworkWorksite(
        worksite: NetworkWorksiteFull,
        syncedAt: Instant,
    ): Boolean {
        val entities = worksite.asEntities()
        return worksiteDaoPlus.syncNetworkWorksite(entities, syncedAt)
    }

    override suspend fun syncNetworkWorksite(networkWorksiteId: Long) {
        val syncedAt = Clock.System.now()
        // TODO Sync related data including secondary, work type requests, ...
        dataSource.getWorksite(networkWorksiteId)?.let { networkWorksite ->
            syncNetworkWorksite(networkWorksite, syncedAt)
        }
    }

    override suspend fun pullWorkTypeRequests(networkWorksiteId: Long) {
        try {
            val workTypeRequests = dataSource.getWorkTypeRequests(networkWorksiteId)
            if (workTypeRequests.isNotEmpty()) {
                val worksiteId = worksiteDao.getWorksiteId(networkWorksiteId)
                val entities = workTypeRequests.map { it.asEntity(worksiteId) }
                workTypeTransferRequestDaoPlus.syncUpsert(entities)
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun setRecentWorksite(
        incidentId: Long,
        worksiteId: Long,
        viewStart: Instant,
    ) {
        if (worksiteId <= 0) {
            return
        }

        recentWorksiteDao.upsert(
            RecentWorksiteEntity(
                id = worksiteId,
                incidentId = incidentId,
                viewedAt = viewStart,
            ),
        )
    }

    override suspend fun getRecentWorksites(incidentId: Long, limit: Int): List<Worksite> {
        val orgId = orgId.first()
        return recentWorksiteDao.getRecents(incidentId, limit)
            .map {
                it.asExternalModel(orgId, languageTranslationsRepository)
                    .worksite
            }
    }

    override fun getUnsyncedCounts(worksiteId: Long) =
        worksiteDaoPlus.getUnsyncedChangeCount(worksiteId, MAX_SYNC_TRIES)

    override suspend fun shareWorksite(
        worksiteId: Long,
        emails: List<String>,
        phoneNumbers: List<String>,
        shareMessage: String,
        noClaimReason: String?,
    ): Boolean {
        try {
            val networkWorksiteId = worksiteDao.getWorksiteNetworkId(worksiteId)
            writeApi.shareWorksite(
                networkWorksiteId,
                emails,
                phoneNumbers,
                shareMessage,
                noClaimReason,
            )

            return true
        } catch (e: Exception) {
            logger.logException(e)
        }
        return false
    }

    override suspend fun getTableData(
        incidentId: Long,
        filters: CasesFilter,
        sortBy: WorksiteSortBy,
        coordinates: Pair<Double, Double>?,
        // miles
        searchRadius: Float,
        count: Int,
    ): List<TableDataWorksite> = coroutineScope {
        // TODO Observe if these values change frequently enough
        organizationAffiliates.first()
        organizationLocationAreaBounds.first()
        val affiliateIds = organizationAffiliates.value
        val areaBounds = organizationLocationAreaBounds.value
        val records = worksiteDaoPlus.loadTableWorksites(
            incidentId,
            filters,
            affiliateIds,
            sortBy,
            coordinates,
            // miles
            searchRadius,
            count,
            areaBounds,
        )

        ensureActive()

        val strideCount = 100
        val tableData = mutableListOf<TableDataWorksite>()
        for (i in records.indices) {
            if (i % strideCount == 0) {
                ensureActive()
            }

            val worksite = records[i].asExternalModel()
            val claimStatus = worksite.getClaimStatus(affiliateIds)
            val tableRecord = TableDataWorksite(worksite, claimStatus)
            tableData.add(tableRecord)
        }

        tableData
    }
}
