package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.Worksite

class PropertyInputData(
    private val inputValidator: InputValidator,
    worksite: Worksite,
    residentName: String = worksite.name,
    phoneNumber: String = worksite.phone1,
    phoneNumberSecondary: String = worksite.phone2,
    email: String = worksite.email ?: "",
    autoContactFrequencyT: String = worksite.autoContactFrequencyT,
    autoContactFrequency: AutoContactFrequency = Worksite.autoContactFrequency(autoContactFrequencyT),
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    var residentName by mutableStateOf(residentName)
    var phoneNumber by mutableStateOf(phoneNumber)
    var phoneNumberSecondary by mutableStateOf(phoneNumberSecondary)
    var email by mutableStateOf(email)
    var autoContactFrequency by mutableStateOf(autoContactFrequency)

    var residentNameError by mutableStateOf("")
    var phoneNumberError by mutableStateOf("")
    var emailError by mutableStateOf("")
    var frequencyError by mutableStateOf("")

    val hasChange =
        residentName != worksiteIn.address ||
                phoneNumber != worksiteIn.phone1 ||
                phoneNumberSecondary != worksiteIn.phone2 ||
                email != worksiteIn.email ||
                autoContactFrequency != worksiteIn.autoContactFrequency

    private fun resetValidity() {
        residentNameError = ""
        residentNameError = ""
        emailError = ""
        frequencyError = ""
    }

    fun validate(): Boolean {
        resetValidity()

        // TODO Use resources for all literals below

        if (residentName.isEmpty()) {
            residentNameError = "Enter a name"
            return false
        }
        if (phoneNumber.isEmpty()) {
            phoneNumberError = "Enter a phone number"
            return false
        }
        if (!inputValidator.validateEmailAddress(email)) {
            emailError = "Enter a valid email address"
            return false
        }
        if (autoContactFrequency == AutoContactFrequency.None) {
            frequencyError = "Select an auto frequency"
            return false
        }
        return true
    }

    override fun updateCase(worksite: Worksite): Worksite? {
        if (!validate()) {
            return null
        }

        return worksite.copy(
            name = residentName,
            phone1 = phoneNumber,
            phone2 = phoneNumberSecondary,
            email = email,
            autoContactFrequencyT = autoContactFrequency.literal,
        )
    }
}
