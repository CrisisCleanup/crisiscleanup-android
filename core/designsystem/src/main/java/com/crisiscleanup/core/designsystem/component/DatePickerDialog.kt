package com.crisiscleanup.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.navigationContainerColor
import com.crisiscleanup.core.designsystem.theme.primaryOrangeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedMillis: Long? = null,
    onCloseDialog: (Long?) -> Unit = {},
) {
    // val year = Calendar.getInstance().get(Calendar.YEAR)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedMillis,
        // TODO There is a bug when the year range is set where cycling back in year very
        //      quickly in succession will cause a crash
        //      java.lang.IllegalArgumentException: Index should be non-negative (-1)
        // yearRange = year until (year + 4),
    )
    val confirmEnabled = remember {
        derivedStateOf {
            datePickerState.selectedDateMillis != null
        }
    }
    val translator = LocalAppTranslator.current
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = {
            onCloseDialog(null)
        },
        confirmButton = {
            CrisisCleanupTextButton(
                onClick = {
                    onCloseDialog(datePickerState.selectedDateMillis)
                },
                enabled = confirmEnabled.value,
                text = translator("actions.ok"),
            )
        },
        dismissButton = {
            CrisisCleanupTextButton(
                onClick = { onCloseDialog(null) },
                text = translator("actions.cancel"),
            )
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    selectedMillis: Pair<Long, Long>? = null,
    onCloseDialog: (Pair<Long, Long>?) -> Unit = {},
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
) {
    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = selectedMillis?.first,
        initialSelectedEndDateMillis = selectedMillis?.second,
    )
    val confirmEnabled = remember {
        derivedStateOf {
            datePickerState.selectedStartDateMillis != null &&
                datePickerState.selectedEndDateMillis != null
        }
    }
    val translator = LocalAppTranslator.current
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = {
            onCloseDialog(selectedMillis)
        },
        confirmButton = {},
        dismissButton = null,
    ) {
        Column {
            Row(
                modifier = listItemModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrisisCleanupIconButton(
                    imageVector = CrisisCleanupIcons.Close,
                    onClick = { onCloseDialog(selectedMillis) },
                )
                Spacer(modifier = Modifier.weight(1f))
                CrisisCleanupTextButton(
                    onClick = {
                        val startMillis = datePickerState.selectedStartDateMillis
                        val endMillis = datePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            onCloseDialog(Pair(startMillis, endMillis))
                        }
                    },
                    enabled = confirmEnabled.value,
                    text = translator("actions.save"),
                )
            }

            DateRangePicker(
                state = datePickerState,
                title = null,
                headline = {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = datePickerState.selectedStartDateMillis,
                        selectedEndDateMillis = datePickerState.selectedEndDateMillis,
                        displayMode = datePickerState.displayMode,
                        dateFormatter,
                        modifier = listItemModifier.padding(vertical = 8.dp),
                    )
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    dayInSelectionRangeContainerColor = navigationContainerColor,
                    dayInSelectionRangeContentColor = primaryOrangeColor,
                ),
            )
        }
    }
}
