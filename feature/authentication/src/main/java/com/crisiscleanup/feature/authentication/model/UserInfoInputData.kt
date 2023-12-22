package com.crisiscleanup.feature.authentication.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.model.data.LanguageIdName

class UserInfoInputData(
    emailAddress: String = "",
) {
    var emailAddress by mutableStateOf(emailAddress)
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var title by mutableStateOf("")
    var phone by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var language by mutableStateOf(LanguageIdName(0, ""))

    var emailAddressError = ""
    var firstNameError = ""
    var lastNameError = ""
    var phoneError = ""
    var passwordError = ""
    var confirmPasswordError = ""

    val hasError: Boolean
        get() = emailAddressError.isNotBlank() ||
            firstNameError.isNotBlank() ||
            lastNameError.isNotBlank() ||
            phoneError.isNotBlank() ||
            passwordError.isNotBlank() ||
            confirmPasswordError.isNotBlank()

    fun clearErrors() {
        emailAddressError = ""
        firstNameError = ""
        lastNameError = ""
        phoneError = ""
        passwordError = ""
        confirmPasswordError = ""
    }

    fun validateInput(inputValidator: InputValidator, translator: KeyResourceTranslator) {
        if (!inputValidator.validateEmailAddress(emailAddress)) {
            emailAddressError = translator("invitationSignup.email_error")
        }

        if (firstName.isBlank()) {
            firstNameError = translator("invitationSignup.first_name_required")
        }

        if (lastName.isBlank()) {
            lastNameError = translator("invitationSignup.last_name_required")
        }

        if (password.trim().length < 8) {
            passwordError = translator("invitationSignup.password_length_error")
        }

        if (password != confirmPassword) {
            confirmPasswordError = translator("invitationSignup.password_match_error")
        }

        if (phone.isBlank()) {
            phoneError = translator("invitationSignup.mobile_error")
        }

        // Language defaults to US English
    }
}
