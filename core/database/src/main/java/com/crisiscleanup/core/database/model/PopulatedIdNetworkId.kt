package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo

data class PopulatedIdNetworkId(
    val id: Long,
    @ColumnInfo("network_id")
    val networkId: Long,
)

fun List<PopulatedIdNetworkId>.asLookup() = associate { it.id to it.networkId }
