package com.crisiscleanup.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

fun Context.openDialer(phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null))
        startActivity(intent)
    } catch (t: Throwable) {
        Log.w("open-dialer", t.message ?: "Unknown exception")
    }
}

fun Context.openMaps(locationQuery: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationQuery))
        startActivity(intent)
    } catch (t: Throwable) {
        Log.w("open-maps", t.message ?: "Unknown exception")
    }
}