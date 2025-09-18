package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.AppMaintenanceData
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppMaintenanceDataSource @Inject constructor(
    private val appMaintenance: DataStore<AppMaintenance>,
) {
    val maintenanceData = appMaintenance.data
        .map {
            AppMaintenanceData(
                ftsRebuildVersion = it.ftsRebuildVersion,
            )
        }

    suspend fun setFtsRebuildVersion(version: Long) {
        appMaintenance.updateData {
            it.copy { ftsRebuildVersion = version }
        }
    }
}