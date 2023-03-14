package com.crisiscleanup.core.common

import android.util.Patterns
import javax.inject.Inject

/**
 * Validates various types of user input
 */
interface InputValidator {
    fun validateEmailAddress(emailAddress: String): Boolean
}

class CommonInputValidator @Inject constructor() : InputValidator {
    override fun validateEmailAddress(emailAddress: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()
    }
}