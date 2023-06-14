package com.crisiscleanup.core.database.dao.fts

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.withTransaction
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.PopulatedIncidentMatch
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.util.ftsGlobEnds
import com.crisiscleanup.core.database.util.ftsSanitize
import com.crisiscleanup.core.database.util.ftsSanitizeAsToken
import com.crisiscleanup.core.database.util.intArray
import com.crisiscleanup.core.database.util.okapiBm25Score
import com.crisiscleanup.core.model.data.IncidentIdNameType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

@Entity(
    "incident_fts",
)
@Fts4(contentEntity = IncidentEntity::class)
data class IncidentFtsEntity(
    val name: String,
    @ColumnInfo("short_name", defaultValue = "")
    val shortName: String,
    @ColumnInfo("incident_type", defaultValue = "")
    val type: String,
)

data class PopulatedIncidentIdNameMatchInfo(
    @Embedded
    val incident: PopulatedIncidentMatch,
    @ColumnInfo("match_info")
    val matchInfo: ByteArray,
) {
    private val matchInfoInts by lazy {
        matchInfo.intArray
    }

    val sortScore by lazy {
        matchInfoInts.okapiBm25Score(0) * 3 +
                matchInfoInts.okapiBm25Score(1) * 2 +
                matchInfoInts.okapiBm25Score(2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PopulatedIncidentIdNameMatchInfo

        if (incident != other.incident) return false
        return matchInfo.contentEquals(other.matchInfo)
    }

    override fun hashCode(): Int {
        var result = incident.hashCode()
        result = 31 * result + matchInfo.contentHashCode()
        return result
    }
}

suspend fun IncidentDaoPlus.rebuildIncidentFts(force: Boolean = false) = db.withTransaction {
    with(db.incidentDao()) {
        var rebuild = force
        if (!force) {
            getRandomIncidentName()?.let { incidentName ->
                val ftsMatch = matchIncidentTokens(incidentName.ftsSanitizeAsToken)
                rebuild = ftsMatch.isEmpty()
            }
        }
        if (rebuild) {
            rebuildIncidentFts()
        }
    }
}

suspend fun IncidentDaoPlus.getMatchingIncidents(q: String): List<IncidentIdNameType> =
    coroutineScope {
        db.withTransaction {
            val results = db.incidentDao()
                .matchIncidentTokens(q.ftsSanitize.ftsGlobEnds)

            ensureActive()

            // TODO ensureActive() between (strides of) score computations
            results
                .sortedByDescending { it.sortScore }
                .map { it.incident.asExternalModel() }
        }
    }