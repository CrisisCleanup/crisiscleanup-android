package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.model.data.CROSS_STREET_FIELD_KEY
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.feature.caseeditor.util.summarizeAddress
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow

class LocationInputData(
    private val translator: KeyResourceTranslator,
    worksite: Worksite,
) : CaseDataWriter {
    private var referenceWorksite = worksite

    var locationQuery = MutableStateFlow("")
    val coordinates = MutableStateFlow(worksite.coordinates)
    var streetAddress by mutableStateOf(worksite.address)
    var zipCode by mutableStateOf(worksite.postalCode)
    var city by mutableStateOf(worksite.city)
    var county by mutableStateOf(worksite.county)
    var state by mutableStateOf(worksite.state)
    var hasWrongLocation by mutableStateOf(worksite.hasWrongLocationFlag)
    var crossStreetNearbyLandmark by mutableStateOf(worksite.crossStreetNearbyLandmark)

    val addressSummary: Collection<String>
        get() = summarizeAddress(streetAddress, zipCode, county, city, state)
    var isEditingAddress by mutableStateOf(false)

    var streetAddressError by mutableStateOf("")
    var zipCodeError by mutableStateOf("")
    var cityError by mutableStateOf("")
    var countyError by mutableStateOf("")
    var stateError by mutableStateOf("")

    private val isIncompleteAddress: Boolean
        get() = streetAddress.isBlank() ||
            zipCode.isBlank() ||
            city.isBlank() ||
            county.isBlank() ||
            state.isBlank()

    val isBlankAddress: Boolean
        get() = streetAddress.isBlank() &&
            zipCode.isBlank() &&
            city.isBlank() &&
            county.isBlank() &&
            state.isBlank()

    var wasGeocodeAddressSelected by mutableStateOf(false)

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
        return this.coordinates.value != worksite.coordinates ||
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

    fun getUserErrorMessage(): Pair<Boolean, String> {
        val translationKeys = mutableListOf<String>()
        var isAddressError = true

        if (coordinates.value == LatLng(0.0, 0.0)) {
            isAddressError = false
            translationKeys.add("caseForm.no_lat_lon_error")
        }
        if (streetAddress.isBlank()) {
            translationKeys.add("caseForm.address_required")
        }
        if (city.isBlank()) {
            translationKeys.add("caseForm.city_required")
        }
        if (county.isBlank()) {
            translationKeys.add("caseForm.county_required")
        }
        if (state.isBlank()) {
            translationKeys.add("caseForm.state_required")
        }
        if (zipCode.isBlank()) {
            translationKeys.add("caseForm.postal_code_required")
        }

        val message = translationKeys.joinToString("\n") { translator(it) }
        return Pair(isAddressError, message)
    }

    private fun validate(): Boolean {
        resetValidity()

        if (streetAddress.isBlank()) {
            streetAddressError = translator("caseForm.address_required")
            return false
        }

        if (city.isBlank()) {
            cityError = translator("caseForm.city_required")
            return false
        }

        if (county.isBlank()) {
            countyError = translator("caseForm.county_required")
            return false
        }

        if (state.isBlank()) {
            stateError = translator("caseForm.state_required")
            return false
        }

        if (zipCode.isBlank()) {
            zipCodeError = translator("caseForm.postal_code_required")
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

    fun assumeLocationAddressChanges(worksite: Worksite) {
        wasGeocodeAddressSelected = true
        referenceWorksite = referenceWorksite.copy(
            latitude = worksite.latitude,
            longitude = worksite.longitude,
        )
        coordinates.value = LatLng(worksite.latitude, worksite.longitude)

        if (
            worksite.address.isNotBlank() ||
            worksite.city.isNotBlank() ||
            worksite.county.isNotBlank() ||
            worksite.postalCode.isNotBlank() ||
            worksite.state.isNotBlank()
        ) {
            referenceWorksite = referenceWorksite.copy(
                address = worksite.address,
                city = worksite.city,
                county = worksite.county,
                postalCode = worksite.postalCode,
                state = worksite.state,
            )
            streetAddress = worksite.address
            zipCode = worksite.postalCode
            city = worksite.city
            county = worksite.county
            state = worksite.state
        }

        if (isIncompleteAddress) {
            isEditingAddress = true
        }
    }
}
