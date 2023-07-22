package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.crisiscleanup.core.network.model.DynamicValue
import com.google.android.gms.maps.model.LatLng

val Worksite.coordinates: LatLng
    get() = LatLng(latitude, longitude)

fun Worksite.copyModifiedFlags(
    hasFlag: Boolean,
    flagPredicate: (WorksiteFlag) -> Boolean,
    flagProducer: () -> WorksiteFlag,
): List<WorksiteFlag>? {
    val outFlags = flags
    val hadFlag = outFlags?.any(flagPredicate) ?: false
    if (hasFlag == hadFlag) {
        return outFlags
    }

    val copyFlags = outFlags?.map(WorksiteFlag::copy)?.toMutableList() ?: mutableListOf()
    if (hasFlag) {
        copyFlags.add(flagProducer())
    } else {
        copyFlags.removeIf(flagPredicate)
    }
    return copyFlags
}

// TODO Complete test coverage
fun Worksite.copyModifiedFormData(
    formValue: WorksiteFormValue,
    targetFieldKey: String,
): Map<String, WorksiteFormValue>? {
    val trimDataValue = formValue.valueString.trim()

    val outFormData = formData
    val prevValue = outFormData?.get(targetFieldKey)
    val isChanged =
        prevValue == null ||
                formValue.isBoolean && formValue.valueBoolean != prevValue.valueBoolean ||
                !formValue.isBoolean && trimDataValue != prevValue.valueString.trim()
    if (!isChanged) {
        return outFormData
    }

    val copyData = mutableMapOf<String, WorksiteFormValue>()
    outFormData?.onEach {
        copyData[it.key] = it.value.copy()
    }

    if (formValue.isBoolean) {
        if (formValue.valueBoolean) {
            copyData[targetFieldKey] = WorksiteFormValue.trueValue
        } else {
            copyData.remove(targetFieldKey)
        }
    } else {
        if (trimDataValue.isBlank()) {
            copyData.remove(targetFieldKey)
        } else {
            copyData[targetFieldKey] = WorksiteFormValue(valueString = trimDataValue)
        }
    }

    return copyData
}

/**
 * Determines if [dynamicFormFields] has data that will change the state of [Worksite.formData]
 *
 * @return TRUE when [dynamicFormFields] has entries differing from the worksite's form data or FALSE otherwise.
 */
fun Worksite.seekChange(dynamicFormFields: Map<String, DynamicValue>): Boolean {
    val worksiteFormData = formData ?: emptyMap()
    for (dynamicFormField in dynamicFormFields) {
        val fieldKey = dynamicFormField.key
        val fieldValue = dynamicFormField.value
        val worksiteData = worksiteFormData[fieldKey]
        if (fieldValue.isBoolean) {
            val worksiteValue = worksiteData?.isBooleanTrue ?: false
            if (worksiteValue != fieldValue.isBooleanTrue) {
                return true
            }
        } else {
            if (fieldValue.valueString.isBlank()) {
                if (worksiteData?.valueString?.isNotBlank() == true) {
                    return true
                }
            } else {
                if (fieldValue.valueString.trim() != worksiteData?.valueString?.trim()) {
                    return true
                }
            }
        }
    }
    return false
}

fun Worksite.copyModifiedFormData(dynamicFormFields: Map<String, DynamicValue>): Map<String, WorksiteFormValue>? {
    if (dynamicFormFields.isEmpty()) {
        return formData
    }

    val mutableFormData = mutableMapOf<String, WorksiteFormValue>()
    for ((key, value) in formData ?: emptyMap()) {
        if (!dynamicFormFields.containsKey(key)) {
            mutableFormData[key] = value
        }
    }

    for ((key, value) in dynamicFormFields) {
        if (value.isBoolean) {
            if (value.valueBoolean) {
                mutableFormData[key] = WorksiteFormValue.trueValue
            }
        } else if (value.valueString.isNotBlank()) {
            mutableFormData[key] = WorksiteFormValue(false, value.valueString.trim())
        }
    }

    return if (mutableFormData.isEmpty()) null else mutableFormData
}