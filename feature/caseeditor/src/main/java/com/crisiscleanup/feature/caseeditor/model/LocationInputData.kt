package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.Worksite
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow

class LocationInputData(
    worksite: Worksite,
    coordinatesIn: LatLng,
    private val resourceProvider: AndroidResourceProvider,
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    var locationQuery = MutableStateFlow("")
    val coordinates = MutableStateFlow(coordinatesIn)

    override fun updateCase() = updateCase(worksiteIn)

    private fun isChanged(worksite: Worksite): Boolean {
        return true
    }

    private fun validate(): Boolean {
        return false
    }

    override fun updateCase(worksite: Worksite): Worksite? {
        if (!isChanged(worksite)) {
            return worksite
        }

        if (!validate()) {
            return null
        }

        return worksite.copy(

        )
    }
}