package com.crisiscleanup.core.common

import android.content.Context
import android.util.Patterns
import dagger.hilt.android.qualifiers.ApplicationContext
import io.michaelrocks.libphonenumber.android.NumberParseException
import javax.inject.Inject
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil as LibPhoneNumber
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil.PhoneNumberFormat as PhoneFormat

/**
 * Validates various types of user input
 */
interface InputValidator {
    fun validateEmailAddress(emailAddress: String): Boolean
    fun validatePhoneNumber(value: String, regionCode: String = "US"): PhoneNumberValidation
    fun hasEmailAddress(text: String): Boolean
}

class CommonInputValidator @Inject constructor(
    @ApplicationContext context: Context,
) : InputValidator {
    private val commonEmailRegex = """\b[^@]+@[^.]+\.[A-Za-z]{2,}\b""".toRegex()

    private val nonDigitRegex = """\D""".toRegex()
    private val phoneUtil by lazy {
        LibPhoneNumber.createInstance(context)
    }

    override fun validateEmailAddress(emailAddress: String) =
        Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()

    override fun validatePhoneNumber(value: String, regionCode: String): PhoneNumberValidation {
        var exception: Exception? = null
        try {
            var phoneNumber = value
            if (!value.trim().startsWith("+")) {
                val digits = value.trim().replace(nonDigitRegex, "")
                if (digits.length != 10) {
                    phoneNumber = "+$value"
                }
            }
            // TODO Use region from device
            phoneUtil.parse(phoneNumber, regionCode)?.let { parsed ->
                if (phoneUtil.isValidNumber(parsed)) {
                    val isUsCountryCode = regionCode == "US" && parsed.countryCode == 1
                    val format = if (isUsCountryCode) {
                        PhoneFormat.NATIONAL
                    } else {
                        PhoneFormat.INTERNATIONAL
                    }
                    val formatted = phoneUtil.format(parsed, format)
                    return PhoneNumberValidation(
                        isValid = true,
                        formatted = formatted,
                    )
                }
            }
        } catch (e: NumberParseException) {
            exception = e
        }
        return PhoneNumberValidation(
            isValid = false,
            formatted = "",
            error = exception,
        )
    }

    override fun hasEmailAddress(text: String) = commonEmailRegex.containsMatchIn(text)
}

data class PhoneNumberValidation(
    val isValid: Boolean,
    val formatted: String,
    val error: Exception? = null,
)
