package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.Worksite
import com.google.android.gms.maps.model.LatLng

class LocationInputData(
    worksite: Worksite,
    coordinatesIn: LatLng,
    private val resourceProvider: AndroidResourceProvider,
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    var locationQuery by mutableStateOf("")
    val coordinates = mutableStateOf(coordinatesIn)

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