package com.crisiscleanup.core.database.dao.fts

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.withTransaction
import com.crisiscleanup.core.database.dao.TeamDaoPlus
import com.crisiscleanup.core.database.model.PopulatedTeam
import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.util.ftsGlobEnds
import com.crisiscleanup.core.database.util.ftsSanitize
import com.crisiscleanup.core.database.util.ftsSanitizeAsToken
import com.crisiscleanup.core.database.util.intArray
import com.crisiscleanup.core.database.util.okapiBm25Score
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.mapLatest

@Entity(
    "team_fts",
)
@Fts4(contentEntity = TeamEntity::class)
data class TeamFtsEntity(
    val name: String,
    val notes: String,
)

data class PopulatedTeamMatchInfo(
    @Embedded
    val team: PopulatedTeam,
    @ColumnInfo("match_info")
    val matchInfo: ByteArray,
) {
    private val matchInfoInts by lazy {
        matchInfo.intArray
    }

    val sortScore by lazy {
        matchInfoInts.okapiBm25Score(0) * 3 +
            matchInfoInts.okapiBm25Score(1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PopulatedTeamMatchInfo

        if (team != other.team) return false
        if (!matchInfo.contentEquals(other.matchInfo)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = team.hashCode()
        result = 31 * result + matchInfo.contentHashCode()
        return result
    }
}

suspend fun TeamDaoPlus.rebuildTeamFts(force: Boolean = false) = db.withTransaction {
    with(db.teamDao()) {
        var rebuild = force
        if (!force) {
            getRandomTeamName()?.let { teamName ->
                val ftsMatch = matchSingleTeamFts(teamName.ftsSanitizeAsToken)
                rebuild = ftsMatch.isEmpty()
            }
        }
        if (rebuild) {
            rebuildTeamFts()
        }
    }
}

suspend fun TeamDaoPlus.streamMatchingTeams(
    q: String,
    incidentId: Long,
) = coroutineScope {
    db.teamDao()
        .streamMatchingTeams(
            q.ftsSanitize.ftsGlobEnds,
            incidentId,
        )
        .mapLatest { matching ->
            matching.sortedByDescending { it.sortScore }
                .map { it.team.asExternalModel() }
        }
}
