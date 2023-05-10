package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteChangeSerializer
import com.crisiscleanup.core.network.model.asSnapshotModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class NetworkWorksiteChangeSerializer @Inject constructor() : WorksiteChangeSerializer {
    override fun serialize(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        flagIdLookup: Map<Long, Long>,
        noteIdLookup: Map<Long, Long>,
        workTypeIdLookup: Map<Long, Long>,
        requestReason: String,
        requestWorkTypes: List<String>,
        releaseReason: String,
        releaseWorkTypes: List<String>,
    ): Pair<Int, String> {
        val snapshotStart = if (worksiteStart.isNew) null
        else worksiteStart.asSnapshotModel(flagIdLookup, noteIdLookup, workTypeIdLookup)
        val snapshotChange =
            worksiteChange.asSnapshotModel(flagIdLookup, noteIdLookup, workTypeIdLookup)
        val change = WorksiteChange(
            snapshotStart,
            snapshotChange,
            WorkTypeTransfer(requestReason, requestWorkTypes),
            WorkTypeTransfer(releaseReason, releaseWorkTypes),
        )
        val serializedChange = Json.encodeToString(change)
        return Pair(WorksiteChangeModelVersion, serializedChange)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WorksiteChangeSerializerModule {
    @Binds
    fun bindsWorksiteChangeSerializer(
        serializer: NetworkWorksiteChangeSerializer
    ): WorksiteChangeSerializer
}