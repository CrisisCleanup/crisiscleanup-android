package com.crisiscleanup.core.model.data

data class LocationAddress(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val county: String,
    val state: String,
    val country: String,
    val zipCode: String,
)
