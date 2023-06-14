package com.crisiscleanup.feature.caseeditor

import android.util.LruCache
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentIdNameType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class QueryIncidentsManager(
    incidentsRepository: IncidentsRepository,
    coroutineScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    cacheMaxSize: Int = 10,
) {
    private val resultCache = LruCache<String, List<IncidentIdNameType>>(cacheMaxSize)

    val incidentQ = MutableStateFlow("")

    private val isLoadingAll = MutableStateFlow(true)
    private val isQuerying = MutableStateFlow(false)
    val isLoading = combine(
        isLoadingAll,
        isQuerying,
    ) { b0, b1 -> b0 || b1 }

    private val allIncidents = incidentsRepository.incidents
        .flowOn(ioDispatcher)

    var incidentLookup = emptyMap<Long, Incident>()
        private set

    private val allIncidentsShort = allIncidents.mapLatest {
        val all = it.map { incident ->
            with(incident) {
                IncidentIdNameType(id, name, shortName, disasterLiteral)
            }
        }

        isLoadingAll.value = false

        all
    }

    private val trimQ = incidentQ.mapLatest(String::trim)
        .stateIn(
            coroutineScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )
    private val matchingIncidents = trimQ
        .mapLatest(String::trim)
        .distinctUntilChanged()
        .throttleLatest(150)
        .mapLatest { q ->
            val incidents = if (q.isEmpty()) {
                emptyList()
            } else {
                val cached = resultCache.get(q)
                if (cached == null) {
                    isQuerying.value = true
                    try {
                        val results = incidentsRepository.getMatchingIncidents(q)
                        resultCache.put(q, results)
                        results
                    } finally {
                        isQuerying.value = false
                    }
                } else {
                    cached
                }
            }
            Pair(q, incidents)
        }
        .flowOn(ioDispatcher)

    val incidentResults = combine(
        allIncidentsShort,
        matchingIncidents,
        ::Pair,
    )
        .filter { (all, matching) ->
            all.isNotEmpty() && matching.first == trimQ.value
        }
        .mapLatest { (all, matching) ->
            val (q, _) = matching
            if (q.isEmpty()) {
                Pair(q, all)
            } else {
                matching
            }
        }
        .stateIn(
            coroutineScope,
            initialValue = Pair("", emptyList()),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        allIncidents.onEach {
            incidentLookup = it.associateBy { incident -> incident.id }
        }
            .launchIn(coroutineScope)
    }
}