package com.crisiscleanup.core.common

import kotlinx.coroutines.flow.SharedFlow

interface PhoneNumberPicker {
    val phoneNumbers: SharedFlow<String>

    fun requestPhoneNumber()
}
