package com.crisiscleanup.feature.caseeditor.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.crisiscleanup.core.common.InputValidator
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.model.data.AutoContactFrequency
import com.crisiscleanup.core.model.data.Worksite
import kotlinx.coroutines.flow.MutableStateFlow

class PropertyInputData(
    private val translator: KeyResourceTranslator,
    private val inputValidator: InputValidator,
    worksite: Worksite,
    residentName: String = worksite.name,
    phoneNumber: String = worksite.phone1,
    phoneNotes: String = worksite.phone1Notes,
    phoneNumberSecondary: String = worksite.phone2,
    phoneNotesSecondary: String = worksite.phone2Notes,
    email: String = worksite.email ?: "",
    autoContactFrequency: AutoContactFrequency = Worksite.autoContactFrequency(worksite.autoContactFrequencyT),
) : CaseDataWriter {
    private val worksiteIn = worksite.copy()

    var residentName = MutableStateFlow(residentName)
    var phoneNumber by mutableStateOf(phoneNumber)
    var phoneNotes by mutableStateOf(phoneNotes)
    var phoneNumberSecondary by mutableStateOf(phoneNumberSecondary)
    var phoneNotesSecondary by mutableStateOf(phoneNotesSecondary)
    var email by mutableStateOf(email)
    var autoContactFrequency by mutableStateOf(autoContactFrequency)

    var residentNameError by mutableStateOf("")
    var phoneNumberError by mutableStateOf("")
    var emailError by mutableStateOf("")

    private fun isChanged(worksite: Worksite) =
        residentName.value.trim() != worksite.name ||
            phoneNumber.trim() != worksite.phone1 ||
            phoneNotes.trim() != worksite.phone1Notes ||
            phoneNumberSecondary.trim() != worksite.phone2 ||
            phoneNotesSecondary.trim() != worksite.phone2Notes ||
            email.trim() != (worksite.email ?: "") ||
            autoContactFrequency != Worksite.autoContactFrequency(worksite.autoContactFrequencyT)

    fun formatPhoneNumber() {
        val result = inputValidator.validatePhoneNumber(phoneNumber)
        if (result.isValid) {
            phoneNumber = result.formatted
        }
    }

    fun formatPhoneNumberSecondary() {
        val result = inputValidator.validatePhoneNumber(phoneNumberSecondary)
        if (result.isValid) {
            phoneNumberSecondary = result.formatted
        }
    }

    private fun resetValidity() {
        residentNameError = ""
        phoneNumberError = ""
        emailError = ""
    }

    fun getUserErrorMessage(): String {
        val messages = mutableListOf<String>()
        if (residentName.value.isBlank()) {
            messages.add(translator("caseForm.name_required"))
        }
        if (phoneNumber.isBlank()) {
            messages.add(translator("caseForm.phone_required"))
        }
        if (!inputValidator.validatePhoneNumber(phoneNumber).isValid) {
            messages.add(translator("info.invalid_phone_number"))
        }
        if (email.isNotBlank() && !inputValidator.hasEmailAddress(email)) {
            messages.add(translator("info.enter_valid_email"))
        }
        return messages.joinToString("\n")
    }

    private fun validate(): Boolean {
        resetValidity()

        if (residentName.value.isBlank()) {
            residentNameError = translator("caseForm.name_required")
            return false
        }
        if (phoneNumber.isBlank()) {
            phoneNumberError = translator("caseForm.phone_required")
            return false
        }
        if (!inputValidator.validatePhoneNumber(phoneNumber).isValid) {
            phoneNumberError = translator("info.invalid_phone_number")
            return false
        }
        if (email.isNotBlank() && !inputValidator.hasEmailAddress(email)) {
            emailError = translator("info.enter_valid_email")
            return false
        }
        return true
    }

    override fun updateCase() = updateCase(worksiteIn)

    private fun updateCase(worksite: Worksite, validate: Boolean): Worksite? {
        if (!isChanged(worksite)) {
            return worksite
        }

        if (validate && !validate()) {
            return null
        }

        return worksite.copy(
            name = residentName.value.trim(),
            phone1 = phoneNumber.trim(),
            phone1Notes = phoneNotes.trim(),
            phone2 = phoneNumberSecondary.trim(),
            phone2Notes = phoneNotesSecondary.trim(),
            email = email.trim(),
            autoContactFrequencyT = autoContactFrequency.literal,
        )
    }

    override fun updateCase(worksite: Worksite) = updateCase(worksite, true)

    override fun copyCase(worksite: Worksite) = updateCase(worksite, false)!!
}
