package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.maps.model.LatLng

fun LocationAddress.toLatLng() = LatLng(latitude, longitude)
