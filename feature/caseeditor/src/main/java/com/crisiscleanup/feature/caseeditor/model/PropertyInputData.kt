package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.R

class PropertyInputData(
    private val inputValidator: InputValidator,
    worksite: Worksite,
    private val resourceProvider: AndroidResourceProvider,
    residentName: String = worksite.name,
    phoneNumber: String = worksite.phone1,
    phoneNumberSecondary: String = worksite.phone2,
    email: String = worksite.email ?: "",
    autoContactFrequency: AutoContactFrequency = Worksite.autoContactFrequency(worksite.autoContactFrequencyT),
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

    private fun isChanged(worksite: Worksite) =
        residentName != worksite.name ||
                phoneNumber != worksite.phone1 ||
                phoneNumberSecondary != worksite.phone2 ||
                email != (worksite.email ?: "") ||
                autoContactFrequency != Worksite.autoContactFrequency(worksite.autoContactFrequencyT)

    private fun resetValidity() {
        residentNameError = ""
        phoneNumberError = ""
        emailError = ""
        frequencyError = ""
    }

    fun validate(): Boolean {
        resetValidity()

        if (residentName.isEmpty()) {
            residentNameError = resourceProvider.getString(R.string.name_is_required)
            return false
        }
        if (phoneNumber.isEmpty()) {
            phoneNumberError = resourceProvider.getString(R.string.phone_is_required)
            return false
        }
        if (email.isNotEmpty() && !inputValidator.validateEmailAddress(email)) {
            emailError = resourceProvider.getString(R.string.invalid_email)
            return false
        }
        if (autoContactFrequency == AutoContactFrequency.None) {
            frequencyError = resourceProvider.getString(R.string.invalid_auto_frequency)
            return false
        }
        return true
    }

    override fun updateCase() = updateCase(worksiteIn)

    override fun updateCase(worksite: Worksite): Worksite? {
        if (!isChanged(worksite)) {
            return worksite
        }

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
