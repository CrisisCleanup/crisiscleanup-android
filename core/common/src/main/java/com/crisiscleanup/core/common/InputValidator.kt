package com.crisiscleanup.core.common

import android.util.Patterns
import javax.inject.Inject

/**
 * Validates various types of user input
 */
interface InputValidator {
    fun validateEmailAddress(emailAddress: String): Boolean
    fun validatePhoneNumber(value: String, allowSpaces: Boolean = true): Boolean
}

class CommonInputValidator @Inject constructor() : InputValidator {
    private val phoneNumbersRegex = """^\+?[\d-]+$""".toRegex()
    private val phoneNumbersAndSpacesRegex = """^\+?[\d\s-]+$""".toRegex()

    override fun validateEmailAddress(emailAddress: String) =
        Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()

    override fun validatePhoneNumber(value: String, allowSpaces: Boolean) =
        if (allowSpaces) phoneNumbersAndSpacesRegex.matches(value)
        else phoneNumbersRegex.matches(value)

}