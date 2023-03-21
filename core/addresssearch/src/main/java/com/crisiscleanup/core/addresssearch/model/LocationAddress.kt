package com.crisiscleanup.core.addresssearch.model

import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.maps.model.LatLng

fun LocationAddress.toLatLng() = LatLng(latitude, longitude)