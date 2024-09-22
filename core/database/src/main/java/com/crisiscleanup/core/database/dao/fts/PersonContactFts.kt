package com.crisiscleanup.core.database.dao.fts

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.withTransaction
import com.crisiscleanup.core.database.dao.PersonContactDaoPlus
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PopulatedPersonContactMatch
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.util.ftsGlobEnds
import com.crisiscleanup.core.database.util.ftsSanitize
import com.crisiscleanup.core.database.util.intArray
import com.crisiscleanup.core.database.util.okapiBm25Score
import com.crisiscleanup.core.model.data.PersonOrganization
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive

@Entity(
    "person_contact_fts",
)
@Fts4(contentEntity = PersonContactEntity::class)
data class PersonContactFtsEntity(
    @ColumnInfo("first_name", defaultValue = "")
    val firstName: String,
    @ColumnInfo("last_name", defaultValue = "")
    val lastName: String,
    val email: String,
    val mobile: String,
)

data class PopulatedPersonContactIdNameMatchInfo(
    @Embedded
    val personContact: PopulatedPersonContactMatch,
    @ColumnInfo("match_info")
    val matchInfo: ByteArray,
) {
    private val matchInfoInts by lazy {
        matchInfo.intArray
    }

    val sortScore by lazy {
        matchInfoInts.okapiBm25Score(0) * 3 +
            matchInfoInts.okapiBm25Score(1) * 3 +
            matchInfoInts.okapiBm25Score(2) +
            matchInfoInts.okapiBm25Score(3)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PopulatedPersonContactIdNameMatchInfo

        if (personContact != other.personContact) return false
        if (!matchInfo.contentEquals(other.matchInfo)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = personContact.hashCode()
        result = 31 * result + matchInfo.contentHashCode()
        return result
    }
}

suspend fun PersonContactDaoPlus.rebuildPersonContactFts(force: Boolean = false) =
    db.withTransaction {
        with(db.personContactDao()) {
            var rebuild = force
            if (!force) {
                val sourceCount = getPersonContactCount()
                if (sourceCount > 0 && getPersonContactFtsCount() < sourceCount) {
                    rebuild = true
                }
            }
            if (rebuild) {
                rebuildPersonContactFts()
            }
        }
    }

suspend fun PersonContactDaoPlus.getMatchingTeamMembers(
    q: String,
    incidentId: Long,
    organizationId: Long,
): List<PersonOrganization> = coroutineScope {
    db.withTransaction {
        val results = db.personContactDao()
            .matchIncidentOrganizationPersonContactTokens(
                q.ftsSanitize.ftsGlobEnds,
                incidentId,
                organizationId,
            )

        ensureActive()

        results
            .sortedByDescending { it.sortScore }
            .map { it.personContact.asExternalModel() }
    }
}
