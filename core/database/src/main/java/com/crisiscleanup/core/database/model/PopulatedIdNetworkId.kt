package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo

data class PopulatedIdNetworkId(
    val id: Long,
    @ColumnInfo("network_id")
    val networkId: Long,
)