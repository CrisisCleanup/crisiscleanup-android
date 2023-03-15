package com.crisiscleanup.core.mapmarker.util

import com.google.android.gms.maps.model.LatLng

fun Pair<Double, Double>.toLatLng() = LatLng(first, second)

// TODO Invoke map changes more elegantly
fun LatLng.smallOffset() = LatLng(
    latitude,
    longitude + Math.random() * 1e-9,
)