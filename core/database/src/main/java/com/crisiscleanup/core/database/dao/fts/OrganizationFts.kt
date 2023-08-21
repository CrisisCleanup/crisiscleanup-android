package com.crisiscleanup.core.database.dao.fts

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.withTransaction
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.util.ftsGlobEnds
import com.crisiscleanup.core.database.util.ftsSanitize
import com.crisiscleanup.core.database.util.ftsSanitizeAsToken
import com.crisiscleanup.core.database.util.intArray
import com.crisiscleanup.core.database.util.okapiBm25Score
import com.crisiscleanup.core.model.data.OrganizationIdName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

@Entity(
    "incident_organization_fts",
)
@Fts4(contentEntity = IncidentOrganizationEntity::class)
data class IncidentOrganizationFtsEntity(
    val name: String,
)

data class PopulatedOrganizationIdNameMatchInfo(
    @Embedded
    val idName: OrganizationIdName,
    @ColumnInfo("match_info")
    val matchInfo: ByteArray,
) {
    private val matchInfoInts by lazy {
        matchInfo.intArray
    }

    val sortScore by lazy {
        matchInfoInts.okapiBm25Score(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PopulatedOrganizationIdNameMatchInfo

        if (idName != other.idName) return false
        return matchInfo.contentEquals(other.matchInfo)
    }

    override fun hashCode(): Int {
        var result = idName.hashCode()
        result = 31 * result + matchInfo.contentHashCode()
        return result
    }
}

suspend fun IncidentOrganizationDaoPlus.rebuildOrganizationFts(force: Boolean = false) =
    db.withTransaction {
        with(db.incidentOrganizationDao()) {
            var rebuild = force
            if (!force) {
                getRandomOrganizationName()?.let { orgName ->
                    val ftsMatch = matchOrganizationName(orgName.ftsSanitizeAsToken)
                    rebuild = ftsMatch.isEmpty()
                }
            }
            if (rebuild) {
                rebuildOrganizationFts()
            }
        }
    }

suspend fun IncidentOrganizationDaoPlus.getMatchingOrganizations(q: String): List<OrganizationIdName> =
    coroutineScope {
        db.withTransaction {
            val results = db.incidentOrganizationDao()
                .matchOrganizationName(q.ftsSanitize.ftsGlobEnds)

            ensureActive()

            // TODO ensureActive() between (strides of) score computations
            results
                .sortedByDescending { it.sortScore }
                .map(PopulatedOrganizationIdNameMatchInfo::idName)
        }
    }
