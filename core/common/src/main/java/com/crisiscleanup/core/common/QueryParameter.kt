package com.crisiscleanup.core.common

import android.net.Uri

val Uri.queryParamMap: Map<String, String>
    get() {
        val params = mutableMapOf<String, String>()
        for (key in queryParameterNames) {
            getQueryParameter(key)?.let { value ->
                params[key] = value
            }
        }
        return params
    }