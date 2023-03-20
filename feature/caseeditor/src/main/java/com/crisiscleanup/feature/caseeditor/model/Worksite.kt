package com.crisiscleanup.feature.caseeditor.model

import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteFlag
import com.crisiscleanup.core.model.data.WorksiteFormValue
import com.google.android.gms.maps.model.LatLng

fun Worksite.coordinates() = LatLng(latitude, longitude)

fun Worksite.copyModifiedFlags(
    hasFlag: Boolean,
    flagPredicate: (WorksiteFlag) -> Boolean,
    flagProducer: () -> WorksiteFlag,
): List<WorksiteFlag>? {
    val outFlags: List<WorksiteFlag>? = flags
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
            copyData[targetFieldKey] = WorksiteFormValue(true, "", true)
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