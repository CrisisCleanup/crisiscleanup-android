package com.crisiscleanup.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

fun Context.openDialer(phoneNumber: String): Boolean {
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null))
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        return true
    }
    return false
}

fun Context.openSms(phoneNumber: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", phoneNumber, null))
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        return true
    }
    return false
}

fun Context.openEmail(emailAddress: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", emailAddress, null))
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        return true
    }
    return false
}

fun Context.openMaps(locationQuery: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationQuery))
        startActivity(intent)
    } catch (t: Throwable) {
        Log.w("open-maps", t.message ?: "Unknown exception")
    }
}
