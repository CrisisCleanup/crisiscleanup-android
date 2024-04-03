package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.BoundedSyncedWorksiteIds
import com.crisiscleanup.core.database.model.CoordinateGridQuery
import com.crisiscleanup.core.database.model.NetworkFileEntity
import com.crisiscleanup.core.database.model.PopulatedFilterDataWorksite
import com.crisiscleanup.core.database.model.PopulatedTableDataWorksite
import com.crisiscleanup.core.database.model.SwNeBounds
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorksiteEntities
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteLocalModifiedAt
import com.crisiscleanup.core.database.model.WorksiteNetworkFileCrossRef
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.filter
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.IncidentIdWorksiteCount
import com.crisiscleanup.core.model.data.OrganizationLocationAreaBounds
import com.crisiscleanup.core.model.data.WorksiteSortBy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

class WorksiteDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
    private val syncLogger: SyncLogger,
) {
    private suspend fun getWorksiteLocalModifiedAt(
        networkWorksiteIds: Set<Long>,
    ) = db.withTransaction {
        val worksitesUpdatedAt = db.worksiteDao().getWorksitesLocalModifiedAt(
            networkWorksiteIds,
        )
        return@withTransaction worksitesUpdatedAt.associateBy { it.networkId }
    }

    /**
     * Syncs a worksite work types
     *
     * Deletes existing work types not specified and upserts work types specified.
     */
    private suspend fun syncWorkTypes(
        worksiteId: Long,
        unassociatedWorkTypes: List<WorkTypeEntity>,
    ) = db.withTransaction {
        if (unassociatedWorkTypes.isEmpty()) {
            return@withTransaction
        }

        val worksiteWorkTypes = unassociatedWorkTypes.map { it.copy(worksiteId = worksiteId) }
        val networkIds = worksiteWorkTypes.map(WorkTypeEntity::networkId)
        val workTypeDao = db.workTypeDao()
        workTypeDao.syncDeleteUnspecified(worksiteId, networkIds)
        val daoPlus = WorkTypeDaoPlus(db)
        daoPlus.syncUpsert(worksiteWorkTypes)
    }

    private suspend fun syncFormData(
        worksiteId: Long,
        unassociatedFormData: List<WorksiteFormDataEntity>,
    ) = db.withTransaction {
        if (unassociatedFormData.isEmpty()) {
            return@withTransaction
        }

        val worksiteFormData = unassociatedFormData.map { it.copy(worksiteId = worksiteId) }
        val fieldKeys = worksiteFormData.map(WorksiteFormDataEntity::fieldKey)
        val formDataDao = db.worksiteFormDataDao()
        formDataDao.deleteUnspecifiedKeys(worksiteId, fieldKeys)
        formDataDao.upsert(worksiteFormData)
    }

    private suspend fun syncFlags(
        worksiteId: Long,
        unassociatedFlags: List<WorksiteFlagEntity>,
    ) = db.withTransaction {
        val flags = unassociatedFlags.map { it.copy(worksiteId = worksiteId) }
        val flagDao = db.worksiteFlagDao()
        val reasons = flags.map(WorksiteFlagEntity::reasonT)
        flagDao.syncDeleteUnspecified(worksiteId, reasons)
        val daoPlus = WorksiteFlagDaoPlus(db)
        daoPlus.syncUpsert(flags)
    }

    private suspend fun syncNotes(
        worksiteId: Long,
        unassociatedNotes: List<WorksiteNoteEntity>,
    ) = db.withTransaction {
        if (unassociatedNotes.isEmpty()) {
            return@withTransaction
        }

        val notes = unassociatedNotes.map { it.copy(worksiteId = worksiteId) }
        val networkIds = notes.map(WorksiteNoteEntity::networkId)
        val noteDao = db.worksiteNoteDao()
        noteDao.syncDeleteUnspecified(worksiteId, networkIds)
        val daoPlus = WorksiteNoteDaoPlus(db)
        daoPlus.syncUpsert(notes)
    }

    // TODO Write tests
    private suspend fun syncFiles(
        worksiteId: Long,
        files: List<NetworkFileEntity>,
    ) = db.withTransaction {
        if (files.isEmpty()) {
            return@withTransaction
        }

        val networkFileDao = db.networkFileDao()
        networkFileDao.upsert(files)
        val ids = files.map(NetworkFileEntity::id).toSet()
        networkFileDao.deleteDeleted(worksiteId, ids)
        networkFileDao.deleteUnspecifiedCrossReferences(worksiteId, ids)
        val networkFileCrossReferences = ids.map { WorksiteNetworkFileCrossRef(worksiteId, it) }
        networkFileDao.insertIgnoreCrossReferences(networkFileCrossReferences)
    }

    private suspend fun syncWorksite(
        worksite: WorksiteEntity,
        modifiedAt: WorksiteLocalModifiedAt?,
        workTypes: List<WorkTypeEntity>,
        syncedAt: Instant,
        formData: List<WorksiteFormDataEntity>? = null,
        flags: List<WorksiteFlagEntity>? = null,
        notes: List<WorksiteNoteEntity>? = null,
        files: List<NetworkFileEntity>? = null,
        // TODO Test coverage
        keepKeyWorkType: Boolean = false,
    ) = db.withTransaction {
        val worksiteDao = db.worksiteDao()

        val isLocallyModified = modifiedAt?.isLocallyModified ?: false
        if (modifiedAt == null) {
            val id = worksiteDao.insertOrRollbackWorksiteRoot(
                syncedAt,
                networkId = worksite.networkId,
                incidentId = worksite.incidentId,
            )
            worksiteDao.insert(worksite.copy(id = id))

            syncWorkTypes(id, workTypes)
            formData?.let { syncFormData(id, it) }
            flags?.let { syncFlags(id, it) }
            notes?.let { syncNotes(id, it) }
            files?.let { syncFiles(id, it) }

            return@withTransaction true
        } else if (!isLocallyModified) {
            if (worksiteDao.getRootCount(
                    id = modifiedAt.id,
                    expectedLocalModifiedAt = modifiedAt.localModifiedAt,
                    networkId = worksite.networkId,
                ) == 0
            ) {
                throw Exception("Worksite has been changed since local modified state was fetched")
            }
            worksiteDao.syncUpdateWorksiteRoot(
                id = modifiedAt.id,
                expectedLocalModifiedAt = modifiedAt.localModifiedAt,
                syncedAt = syncedAt,
                networkId = worksite.networkId,
                incidentId = worksite.incidentId,
            )
            with(worksite) {
                worksiteDao.syncUpdateWorksite(
                    id = modifiedAt.id,
                    networkId = networkId,
                    incidentId = worksite.incidentId,
                    address = address,
                    autoContactFrequencyT = autoContactFrequencyT,
                    caseNumber = caseNumber,
                    caseNumberOrder = caseNumberOrder,
                    city = city,
                    county = county,
                    createdAt = createdAt,
                    email = email,
                    favoriteId = favoriteId,
                    keyWorkTypeType = if (keepKeyWorkType) "" else keyWorkTypeType,
                    keyWorkTypeOrgClaim = if (keepKeyWorkType) -1 else keyWorkTypeOrgClaim,
                    keyWorkTypeStatus = if (keepKeyWorkType) "" else keyWorkTypeStatus,
                    latitude = latitude,
                    longitude = longitude,
                    name = name,
                    phone1 = phone1,
                    phone2 = phone2,
                    plusCode = plusCode,
                    postalCode = postalCode,
                    reportedBy = reportedBy,
                    state = state,
                    svi = svi,
                    what3Words = what3Words,
                    updatedAt = updatedAt,
                )
            }

            // Should return a valid ID if UPDATE OR ROLLBACK query succeeded
            val worksiteId = worksiteDao.getWorksiteId(worksite.networkId)

            syncWorkTypes(worksiteId, workTypes)
            formData?.let { syncFormData(worksiteId, it) }
            flags?.let { syncFlags(worksiteId, it) }
            notes?.let { syncNotes(worksiteId, it) }
            files?.let { syncFiles(worksiteId, it) }

            return@withTransaction true
        } else {
            // Resolving changes at this point is not worth the complexity.
            // Defer to worksite (snapshot) changes resolving successfully and completely.
            syncLogger.log("Skip sync overwriting locally modified worksite ${worksite.id} (${worksite.networkId})")
        }

        return@withTransaction false
    }

    private fun throwSizeMismatch(worksitesSize: Int, size: Int, dataName: String) {
        if (worksitesSize != size) {
            throw Exception("Inconsistent data size. Each worksite must have corresponding $dataName.")
        }
    }

    /**
     * Syncs worksite data skipping worksites where local changes exist
     *
     * @return Number of worksites inserted/updated
     */
    suspend fun syncWorksites(
        worksites: List<WorksiteEntity>,
        worksitesWorkTypes: List<List<WorkTypeEntity>>,
        syncedAt: Instant,
    ) {
        throwSizeMismatch(worksites.size, worksitesWorkTypes.size, "work types")

        val networkWorksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val modifiedAtLookup = getWorksiteLocalModifiedAt(networkWorksiteIds)

            worksites.forEachIndexed { i, worksite ->
                val workTypes = worksitesWorkTypes[i]
                val modifiedAt = modifiedAtLookup[worksite.networkId]
                syncWorksite(
                    worksite,
                    modifiedAt,
                    workTypes,
                    syncedAt,
                )
            }
        }
    }

    // TODO Write tests
    suspend fun syncShortFlags(
        worksites: List<WorksiteEntity>,
        worksitesFlags: List<List<WorksiteFlagEntity>>,
    ) {
        throwSizeMismatch(worksites.size, worksitesFlags.size, "flags")

        val networkWorksiteIds = worksites.map(WorksiteEntity::networkId).toSet()
        db.withTransaction {
            val modifiedAtLookup = getWorksiteLocalModifiedAt(networkWorksiteIds)

            val worksiteDao = db.worksiteDao()
            val flagDao = db.worksiteFlagDao()
            worksitesFlags.forEachIndexed { i, flags ->
                val networkWorksiteId = worksites[i].networkId
                val modifiedAt = modifiedAtLookup[networkWorksiteId]
                val isLocallyModified = modifiedAt?.isLocallyModified ?: false
                if (!isLocallyModified) {
                    val worksiteId = worksiteDao.getWorksiteId(networkWorksiteId)
                    val flagReasons = flags.map(WorksiteFlagEntity::reasonT)
                    flagDao.syncDeleteUnspecified(worksiteId, flagReasons)
                    val updatedFlags = flags.map { it.copy(worksiteId = worksiteId) }
                    flagDao.insertIgnore(updatedFlags)
                }
            }
        }
    }

    // TODO Write tests
    suspend fun syncAdditionalData(
        networkWorksiteIds: List<Long>,
        formDatas: List<List<WorksiteFormDataEntity>>,
        reportedBys: List<Long?>,
    ) {
        throwSizeMismatch(networkWorksiteIds.size, formDatas.size, "form-data")
        throwSizeMismatch(networkWorksiteIds.size, reportedBys.size, "reported-bys")

        val worksiteIdsSet = networkWorksiteIds.toSet()
        db.withTransaction {
            val modifiedAtLookup = getWorksiteLocalModifiedAt(worksiteIdsSet)

            val worksiteDao = db.worksiteDao()
            val formDataDao = db.worksiteFormDataDao()
            formDatas.forEachIndexed { i, formData ->
                val networkWorksiteId = networkWorksiteIds[i]
                val modifiedAt = modifiedAtLookup[networkWorksiteId]
                val isLocallyModified = modifiedAt?.isLocallyModified ?: false
                if (!isLocallyModified) {
                    val worksiteId = worksiteDao.getWorksiteId(networkWorksiteId)
                    val fieldKeys = formData.map(WorksiteFormDataEntity::fieldKey)
                    formDataDao.deleteUnspecifiedKeys(worksiteId, fieldKeys)
                    val updatedFormData = formData.map { it.copy(worksiteId = worksiteId) }
                    formDataDao.upsert(updatedFormData)

                    val reportedBy = reportedBys[i]
                    worksiteDao.syncUpdateAdditionalData(worksiteId, reportedBy)
                }
            }
        }
    }

    suspend fun syncWorksite(
        entities: WorksiteEntities,
        syncedAt: Instant,
    ) = db.withTransaction {
        val core = entities.core
        val modifiedAtLookup = getWorksiteLocalModifiedAt(setOf(core.networkId))
        val modifiedAt = modifiedAtLookup[core.networkId]
        val isUpdated = syncWorksite(
            core,
            modifiedAt,
            entities.workTypes,
            syncedAt,
            entities.formData,
            entities.flags,
            entities.notes,
            entities.files,
            true,
        )

        val worksiteId =
            if (isUpdated) {
                db.worksiteDao().getWorksiteId(core.networkId)
            } else {
                -1
            }
        return@withTransaction Pair(isUpdated, worksiteId)
    }

    suspend fun syncFillWorksite(
        entities: WorksiteEntities,
    ) = db.withTransaction {
        val worksiteDao = db.worksiteDao()
        val (core, flags, formData, notes, workTypes, files) = entities
        val worksiteId = worksiteDao.getWorksiteId(core.networkId)
        if (worksiteId > 0) {
            with(core) {
                worksiteDao.syncFillWorksite(
                    id = worksiteId,
                    autoContactFrequencyT = autoContactFrequencyT,
                    caseNumber = caseNumber,
                    caseNumberOrder = caseNumberOrder,
                    email = email,
                    favoriteId = favoriteId,
                    phone1 = phone1,
                    phone2 = phone2,
                    plusCode = plusCode,
                    svi = svi,
                    reportedBy = reportedBy,
                    what3Words = what3Words,
                )
            }

            val flagDao = db.worksiteFlagDao()
            val flagsReasons = flagDao.getReasons(worksiteId).toSet()
            val newFlags = flags.filter { !flagsReasons.contains(it.reasonT) }
                .map { it.copy(worksiteId = worksiteId) }
            flagDao.insertIgnore(newFlags)

            val formDataDao = db.worksiteFormDataDao()
            val formDataKeys = formDataDao.getDataKeys(worksiteId).toSet()
            val newFormData = formData.filter { !formDataKeys.contains(it.fieldKey) }
                .map { it.copy(worksiteId = worksiteId) }
            formDataDao.upsert(newFormData)

            val noteDao = db.worksiteNoteDao()
            val recentNotes = noteDao.getNotes(worksiteId).map(String::trim).toSet()
            val newNotes = notes.filter { !recentNotes.contains(it.note) }
                .map { it.copy(worksiteId = worksiteId) }
            noteDao.insertIgnore(newNotes)

            val workTypeDao = db.workTypeDao()
            val workTypeKeys = workTypeDao.getWorkTypes(worksiteId).toSet()
            val newWorkTypes = workTypes.filter { !workTypeKeys.contains(it.workType) }
                .map { it.copy(worksiteId = worksiteId) }
            workTypeDao.insertIgnore(newWorkTypes)

            syncFiles(worksiteId, files)

            return@withTransaction true
        }
        return@withTransaction false
    }

    suspend fun syncNetworkWorksite(
        entities: WorksiteEntities,
        syncedAt: Instant,
    ) = db.withTransaction {
        val (isSynced, _) = syncWorksite(entities, syncedAt)
        if (!isSynced) {
            syncFillWorksite(entities)
        }
        return@withTransaction isSynced
    }

    suspend fun syncWorksites(
        worksitesEntities: List<WorksiteEntities>,
        syncedAt: Instant,
    ) = db.withTransaction {
        worksitesEntities.forEach { entities ->
            syncNetworkWorksite(entities, syncedAt)
        }
    }

    fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ): Int {
        val worksiteDao = db.worksiteDao()
        val isLongitudeOrdered = longitudeLeft < longitudeRight
        return if (isLongitudeOrdered) {
            worksiteDao.getWorksitesCount(
                incidentId,
                latitudeSouth,
                latitudeNorth,
                longitudeLeft,
                longitudeRight,
            )
        } else {
            worksiteDao.getWorksitesCountLongitudeCrossover(
                incidentId,
                latitudeSouth,
                latitudeNorth,
                longitudeLeft,
                longitudeRight,
            )
        }
    }

    suspend fun onSyncEnd(
        worksiteId: Long,
        maxSyncTries: Int,
        syncLogger: SyncLogger,
        syncedAt: Instant = Clock.System.now(),
    ) = db.withTransaction {
        val flagChanges = db.worksiteFlagDao().getUnsyncedCount(worksiteId)
        val noteChanges = db.worksiteNoteDao().getUnsyncedCount(worksiteId)
        val workTypeChanges = db.workTypeDao().getUnsyncedCount(worksiteId)
        val changes = db.worksiteChangeDao().getChangeCount(worksiteId, maxSyncTries)
        val hasModification = flagChanges > 0 ||
            noteChanges > 0 ||
            workTypeChanges > 0 ||
            changes > 0
        return@withTransaction if (hasModification) {
            syncLogger.log(
                "Pending changes on sync end",
                details = "flag: $flagChanges\nnote: $noteChanges\nwork type: $workTypeChanges\nchanges: $changes",
            )
            false
        } else {
            db.worksiteDao().setRootUnmodified(worksiteId, syncedAt)
            db.workTypeTransferRequestDao().deleteUnsynced(worksiteId)
            true
        }
    }

    fun getUnsyncedChangeCount(worksiteId: Long, maxSyncTries: Int) = listOf(
        db.worksiteFlagDao().getUnsyncedCount(worksiteId),
        db.worksiteNoteDao().getUnsyncedCount(worksiteId),
        db.workTypeDao().getUnsyncedCount(worksiteId),
        db.worksiteChangeDao().getChangeCount(worksiteId, maxSyncTries),
    )

    suspend fun loadBoundedSyncedWorksiteIds(
        incidentId: Long,
        loadedIds: MutableList<BoundedSyncedWorksiteIds>,
        minLoadCount: Int,
        remainingBounds: List<SwNeBounds>,
        filter: (BoundedSyncedWorksiteIds) -> Boolean,
    ) = db.withTransaction {
        val worksiteDao = db.worksiteDao()

        var boundsIndex = 0
        while (loadedIds.size < minLoadCount && boundsIndex < remainingBounds.size) {
            with(remainingBounds[boundsIndex++]) {
                val worksiteIds = worksiteDao.getBoundedSyncedWorksiteIds(
                    incidentId,
                    south,
                    north,
                    west,
                    east,
                )
                loadedIds.addAll(worksiteIds.filter(filter))
            }
        }

        if (boundsIndex >= remainingBounds.size) {
            emptyList()
        } else {
            remainingBounds.subList(boundsIndex, remainingBounds.size)
        }
    }

    suspend fun loadTableWorksites(
        incidentId: Long,
        filters: CasesFilter,
        organizationAffiliates: Set<Long>,
        sortBy: WorksiteSortBy,
        coordinates: Pair<Double, Double>?,
        // miles
        searchRadius: Float,
        count: Int,
        locationAreaBounds: OrganizationLocationAreaBounds,
    ) = coroutineScope {
        when (sortBy) {
            WorksiteSortBy.Nearest -> {
                if (coordinates == null) {
                    emptyList()
                } else {
                    getNearestTableWorksites(
                        incidentId,
                        count,
                        searchRadius,
                        filters,
                        organizationAffiliates,
                        coordinates,
                        locationAreaBounds,
                    )
                }
            }

            else -> getFilteredTableWorksites(
                sortBy,
                incidentId,
                count,
                filters,
                organizationAffiliates,
                coordinates,
                locationAreaBounds,
            )
        }
    }

    private suspend fun getFilteredTableWorksites(
        sortBy: WorksiteSortBy,
        incidentId: Long,
        count: Int,
        filters: CasesFilter,
        organizationAffiliates: Set<Long>,
        coordinates: Pair<Double, Double>?,
        locationAreaBounds: OrganizationLocationAreaBounds,
    ) = coroutineScope {
        val queryCount = count.coerceAtLeast(100)
        var queryOffset = 0

        val worksiteDao = db.worksiteDao()
        val worksiteData = mutableListOf<PopulatedTableDataWorksite>()
        while (worksiteData.size < queryCount) {
            val records = when (sortBy) {
                WorksiteSortBy.Name -> worksiteDao.getTableWorksitesOrderByName(
                    incidentId,
                    queryCount,
                    queryOffset,
                )

                WorksiteSortBy.City -> worksiteDao.getTableWorksitesOrderByCity(
                    incidentId,
                    queryCount,
                    queryOffset,
                )

                WorksiteSortBy.CountyParish -> worksiteDao.getTableWorksitesOrderByCounty(
                    incidentId,
                    queryCount,
                    queryOffset,
                )

                else -> worksiteDao.getTableWorksitesOrderByCaseNumber(
                    incidentId,
                    queryCount,
                    queryOffset,
                )
            }

            ensureActive()

            val filteredRecords = records.filter(
                filters,
                organizationAffiliates,
                coordinates,
                locationAreaBounds,
            )

            ensureActive()

            worksiteData.addAll(filteredRecords)

            queryOffset += queryCount

            if (sortBy == WorksiteSortBy.Nearest || records.size < queryCount) {
                break
            }
        }

        worksiteData
    }

    private suspend fun getNearestTableWorksites(
        incidentId: Long,
        count: Int,
        // miles
        searchRadius: Float = 100.0f,
        filters: CasesFilter,
        organizationAffiliates: Set<Long>,
        coordinates: Pair<Double, Double>,
        locationAreaBounds: OrganizationLocationAreaBounds,
    ) = coroutineScope {
        val strideCount = 100

        val latitude = coordinates.first
        val longitude = coordinates.second

        val worksiteDao = db.worksiteDao()
        val worksiteCount = worksiteDao.getWorksitesCount(incidentId)

        val boundedWorksites: List<PopulatedTableDataWorksite>
        if (worksiteCount <= count) {
            boundedWorksites = worksiteDao.getTableWorksites(incidentId).filter(
                filters,
                organizationAffiliates,
                coordinates,
                locationAreaBounds,
            )
        } else {
            val r = searchRadius.coerceAtLeast(24.0f)
            val latitudeRadialDegrees = r / 69.0
            val longitudeRadialDegrees = r / 54.6
            val areaBounds = SwNeBounds(
                south = (latitude - latitudeRadialDegrees).coerceAtLeast(-90.0),
                north = (latitude + latitudeRadialDegrees).coerceAtMost(90.0),
                west = (longitude - longitudeRadialDegrees).coerceAtLeast(-180.0),
                east = (longitude + longitudeRadialDegrees).coerceAtMost(180.0),
            )
            val boundedWorksiteRectCount = worksiteDao.getBoundedWorksiteCount(
                incidentId,
                latitudeSouth = areaBounds.south,
                latitudeNorth = areaBounds.north,
                longitudeWest = areaBounds.west,
                longitudeEast = areaBounds.east,
            )
            val gridQuery = CoordinateGridQuery(areaBounds)
            val targetBucketCount = 10
            gridQuery.initializeGrid(boundedWorksiteRectCount, targetBucketCount)

            val maxQueryCount = (count * 1.5).toInt()
            boundedWorksites = loadBoundedTableWorksites(
                incidentId,
                maxQueryCount,
                gridQuery.getSwNeGridCells(),
                filters,
                organizationAffiliates,
                coordinates,
                locationAreaBounds,
            )
        }

        val latRad = latitude.radians
        val lngRad = longitude.radians
        val withDistance = mutableListOf<Pair<PopulatedTableDataWorksite, Double>>()
        for (i in boundedWorksites.indices) {
            val worksite = boundedWorksites[i]
            val entity = worksite.base.entity
            val distance = haversineDistance(
                latRad,
                lngRad,
                entity.latitude.radians,
                entity.longitude.radians,
            )
            withDistance.add(Pair(worksite, distance))
            if (i % strideCount == 0) {
                ensureActive()
            }
        }
        return@coroutineScope withDistance
            .sortedBy { it.second }
            .map { it.first }
    }

    private suspend fun loadBoundedTableWorksites(
        incidentId: Long,
        maxLoadCount: Int,
        remainingBounds: List<SwNeBounds>,
        filters: CasesFilter,
        organizationAffiliates: Set<Long>,
        coordinates: Pair<Double, Double>,
        locationAreaBounds: OrganizationLocationAreaBounds,
    ) = coroutineScope {
        val worksiteDao = db.worksiteDao()

        val loadedWorksites = mutableListOf<PopulatedTableDataWorksite>()

        var boundsIndex = 0
        while (boundsIndex < remainingBounds.size) {
            var records: List<PopulatedTableDataWorksite>
            with(remainingBounds[boundsIndex++]) {
                records = worksiteDao.getTableWorksitesInBounds(
                    incidentId,
                    south,
                    north,
                    west,
                    east,
                )
            }

            ensureActive()

            val filteredRecords = records.filter(
                filters,
                organizationAffiliates,
                coordinates,
                locationAreaBounds,
            )

            ensureActive()

            loadedWorksites.addAll(filteredRecords)

            if (loadedWorksites.size > maxLoadCount) {
                break
            }
        }

        loadedWorksites
    }

    suspend fun getWorksitesCount(
        incidentId: Long,
        totalCount: Int,
        filters: CasesFilter,
        organizationAffiliates: Set<Long>,
        coordinates: Pair<Double, Double>?,
        locationAreaBounds: OrganizationLocationAreaBounds,
    ) = coroutineScope {
        val stride = 2000
        var offset = 0
        var count = 0
        val worksiteDao = db.worksiteDao()
        val latRad = coordinates?.first?.radians
        val lngRad = coordinates?.second?.radians
        while (offset < totalCount) {
            ensureActive()

            val worksites: List<PopulatedFilterDataWorksite>
            if (filters.hasSviFilter) {
                worksites = worksiteDao.getFilterWorksitesBySvi(
                    incidentId,
                    filters.svi,
                    stride,
                    offset,
                )
            } else if (filters.hasUpdatedFilter) {
                worksites = worksiteDao.getFilterWorksitesByUpdatedAfter(
                    incidentId,
                    Clock.System.now().minus(filters.daysAgoUpdated.days),
                    stride,
                    offset,
                )
            } else if (filters.updatedAt != null) {
                worksites = worksiteDao.getFilterWorksitesByUpdatedAt(
                    incidentId,
                    filters.updatedAt!!.first,
                    filters.updatedAt!!.second,
                    stride,
                    offset,
                )
            } else if (filters.createdAt != null) {
                worksites = worksiteDao.getFilterWorksitesByCreatedAt(
                    incidentId,
                    filters.createdAt!!.first,
                    filters.createdAt!!.second,
                    stride,
                    offset,
                )
            } else {
                worksites = worksiteDao.getFilterWorksites(incidentId, stride, offset)
            }
            if (worksites.isEmpty()) {
                break
            }

            ensureActive()

            count += worksites.count {
                it.passesFilter(
                    filters,
                    organizationAffiliates,
                    latRad,
                    lngRad,
                    locationAreaBounds,
                )
            }

            offset += stride
        }

        IncidentIdWorksiteCount(incidentId, totalCount, count)
    }
}
