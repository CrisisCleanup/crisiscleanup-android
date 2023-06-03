package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.model.data.CROSS_STREET_FIELD_KEY
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.feature.caseeditor.R
import com.crisiscleanup.feature.caseeditor.util.summarizeAddress
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow

class LocationInputData(
    worksite: Worksite,
    private val resourceProvider: AndroidResourceProvider,
) : CaseDataWriter {
    private var referenceWorksite = worksite

    var locationQuery = MutableStateFlow("")
    val coordinates = MutableStateFlow(worksite.coordinates())
    var streetAddress by mutableStateOf(worksite.address)
    var zipCode by mutableStateOf(worksite.postalCode)
    var city by mutableStateOf(worksite.city)
    var county by mutableStateOf(worksite.county)
    var state by mutableStateOf(worksite.state)
    var hasWrongLocation by mutableStateOf(worksite.hasWrongLocationFlag)
    var crossStreetNearbyLandmark by mutableStateOf(worksite.crossStreetNearbyLandmark)

    val addressSummary: Collection<String>
        get() = summarizeAddress(streetAddress, zipCode, county, city, state)

    var streetAddressError by mutableStateOf("")
    var zipCodeError by mutableStateOf("")
    var cityError by mutableStateOf("")
    var countyError by mutableStateOf("")
    var stateError by mutableStateOf("")

    val hasAddressError: Boolean
        get() = streetAddressError.isNotBlank() ||
                zipCodeError.isNotBlank() ||
                cityError.isNotBlank() ||
                countyError.isNotBlank() ||
                stateError.isNotBlank()

    val addressChangeWorksite: Worksite
        get() {
            val worksite = referenceWorksite
            val coordinatesSnapshot = coordinates.value
            return worksite.copy(
                latitude = coordinatesSnapshot.latitude,
                longitude = coordinatesSnapshot.longitude,
                address = streetAddress.trim(),
                city = city.trim(),
                county = county.trim(),
                postalCode = zipCode.trim(),
                state = state.trim(),
            )
        }

    private fun isChanged(worksite: Worksite): Boolean {
        return this.coordinates.value != worksite.coordinates() ||
                streetAddress.trim() != worksite.address ||
                zipCode.trim() != worksite.postalCode ||
                city.trim() != worksite.city ||
                county.trim() != worksite.county ||
                state.trim() != worksite.state ||
                hasWrongLocation != worksite.hasWrongLocationFlag ||
                crossStreetNearbyLandmark.trim() != worksite.crossStreetNearbyLandmark
    }

    internal fun resetValidity() {
        streetAddressError = ""
        zipCodeError = ""
        cityError = ""
        countyError = ""
        stateError = ""
    }

    private fun validate(): Boolean {
        resetValidity()

        if (streetAddress.isBlank()) {
            streetAddressError = resourceProvider.getString(R.string.address_is_required)
            return false
        }

        if (zipCode.isBlank()) {
            zipCodeError = resourceProvider.getString(R.string.zipcode_is_required)
            return false
        }

        if (county.isBlank()) {
            countyError = resourceProvider.getString(R.string.county_is_required)
            return false
        }

        if (city.isBlank()) {
            cityError = resourceProvider.getString(R.string.city_is_required)
            return false
        }

        if (state.isBlank()) {
            stateError = resourceProvider.getString(R.string.state_is_required)
            return false
        }

        return true
    }

    override fun updateCase() = updateCase(referenceWorksite)

    private fun updateCase(worksite: Worksite, validate: Boolean): Worksite? {
        if (!isChanged(worksite)) {
            return worksite
        } else if (worksite.isNew && streetAddress.isBlank()) {
            val latLng = coordinates.value
            val copyCoordinates = worksite.copy(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
            )
            if (!isChanged(copyCoordinates)) {
                return worksite
            }
        }

        if (validate && !validate()) {
            if (hasAddressError) {
                hasWrongLocation = true
            }
            return null
        }

        val flags = worksite.copyModifiedFlags(
            hasWrongLocation,
            WorksiteFlag::isWrongLocationFlag,
        ) { WorksiteFlag.wrongLocation() }

        val formData =
            worksite.copyModifiedFormData(
                WorksiteFormValue(valueString = crossStreetNearbyLandmark),
                CROSS_STREET_FIELD_KEY,
            )

        val coordinatesSnapshot = coordinates.value
        return worksite.copy(
            latitude = coordinatesSnapshot.latitude,
            longitude = coordinatesSnapshot.longitude,
            address = streetAddress.trim(),
            city = city.trim(),
            county = county.trim(),
            postalCode = zipCode.trim(),
            state = state.trim(),
            flags = if (flags?.isNotEmpty() == true) flags else null,
            formData = if (formData?.isNotEmpty() == true) formData else null,
        )
    }

    override fun updateCase(worksite: Worksite): Worksite? = updateCase(worksite, true)

    override fun copyCase(worksite: Worksite) = updateCase(worksite, false)!!

    fun assumeLocationAddressChanges(worksite: Worksite, updateAddressProblem: Boolean = false) {
        referenceWorksite = referenceWorksite.copy(
            latitude = worksite.latitude,
            longitude = worksite.longitude,
            address = worksite.address,
            city = worksite.city,
            county = worksite.county,
            postalCode = worksite.postalCode,
            state = worksite.state,
        )
        coordinates.value = LatLng(worksite.latitude, worksite.longitude)
        streetAddress = worksite.address
        zipCode = worksite.postalCode
        city = worksite.city
        county = worksite.county
        state = worksite.state

        if (updateAddressProblem) {
            hasWrongLocation = listOf(
                streetAddress,
                zipCode,
                county,
                city,
                state,
            ).any(String::isBlank)
        }
    }
}