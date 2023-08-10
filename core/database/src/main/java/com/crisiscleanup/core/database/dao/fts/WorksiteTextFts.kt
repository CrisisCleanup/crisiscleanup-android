package com.crisiscleanup.core.database.dao.fts

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.withTransaction
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.util.ftsGlobEnds
import com.crisiscleanup.core.database.util.ftsSanitize
import com.crisiscleanup.core.database.util.ftsSanitizeAsToken
import com.crisiscleanup.core.database.util.intArray
import com.crisiscleanup.core.database.util.okapiBm25Score
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteSummary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

@Entity(
    "worksite_text_fts_b",
)
@Fts4(contentEntity = WorksiteEntity::class)
data class WorksiteTextFtsEntity(
    val address: String,
    @ColumnInfo("case_number")
    val caseNumber: String,
    val city: String,
    val county: String,
    val email: String,
    val name: String,
    val phone1: String,
    val phone2: String,
)

data class PopulatedWorksiteTextMatchInfo(
    @Embedded
    val entity: WorksiteEntity,
    @ColumnInfo("match_info")
    val matchInfo: ByteArray,
) {
    private val matchInfoInts by lazy {
        matchInfo.intArray
    }

    val sortScore by lazy {
        matchInfoInts.okapiBm25Score(0) * 0.9 +
                matchInfoInts.okapiBm25Score(1) +
                matchInfoInts.okapiBm25Score(2) * 0.8 +
                matchInfoInts.okapiBm25Score(3) * 0.7 +
                matchInfoInts.okapiBm25Score(4) * 0.7 +
                matchInfoInts.okapiBm25Score(5) * 0.9 +
                matchInfoInts.okapiBm25Score(6) * 0.6 +
                matchInfoInts.okapiBm25Score(7) * 0.6
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PopulatedWorksiteTextMatchInfo

        if (entity != other.entity) return false
        if (!matchInfo.contentEquals(other.matchInfo)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = entity.hashCode()
        result = 31 * result + matchInfo.contentHashCode()
        return result
    }
}

suspend fun WorksiteDaoPlus.rebuildWorksiteTextFts(force: Boolean = false) =
    db.withTransaction {
        with(db.worksiteDao()) {
            var rebuild = force
            if (!force) {
                getRandomWorksiteCaseNumber()?.let { caseNumber ->
                    val ftsMatch = matchWorksiteTextTokens(caseNumber.ftsSanitizeAsToken)
                    rebuild = ftsMatch.isEmpty()
                }
            }
            if (rebuild) {
                rebuildWorksiteTextFts()
            }
        }
    }

suspend fun WorksiteDaoPlus.getMatchingWorksites(
    incidentId: Long,
    q: String,
    resultCount: Int = 20,
): List<WorksiteSummary> = coroutineScope {
    db.withTransaction {
        val results = db.worksiteDao()
            .matchWorksiteTextTokens(incidentId, q.ftsSanitize.ftsGlobEnds)

        ensureActive()

        results
            .sortedByDescending { it.sortScore }
            .subList(0, resultCount.coerceAtMost(results.size))
            .map { it.entity.asSummary() }
    }
}

private fun WorksiteEntity.asSummary(): WorksiteSummary {
    return WorksiteSummary(
        id,
        networkId,
        name,
        address,
        city,
        state,
        postalCode,
        county,
        caseNumber,
        WorkType(
            0,
            statusLiteral = keyWorkTypeStatus,
            workTypeLiteral = keyWorkTypeType,
        ),
    )
}
