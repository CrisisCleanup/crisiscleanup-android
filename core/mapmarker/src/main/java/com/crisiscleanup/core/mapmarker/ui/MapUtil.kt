package com.crisiscleanup.core.mapmarker.ui

import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings

@Composable
fun rememberMapUiSettings(
    myLocation: Boolean = false,
): MutableState<MapUiSettings> {
    return remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = myLocation,
            )
        )
    }
}

@Composable
fun rememberMapProperties(
    @RawRes mapStyle: Int = 0,
): MutableState<MapProperties> {
    val context = LocalContext.current
    val mapStyleOptions = if (mapStyle == 0) null
    else MapStyleOptions.loadRawResourceStyle(context, mapStyle)
    return remember {
        mutableStateOf(
            MapProperties(
                mapStyleOptions = mapStyleOptions,
            )
        )
    }
}