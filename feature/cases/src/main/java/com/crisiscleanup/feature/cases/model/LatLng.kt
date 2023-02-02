package com.crisiscleanup.feature.cases.model

import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.google.android.gms.maps.model.LatLng

fun WorksiteMapMark.asLatLng() = LatLng(latitude, longitude)