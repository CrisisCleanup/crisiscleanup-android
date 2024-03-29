package com.crisiscleanup.feature.caseeditor.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.filterNotBlankTrim
import com.crisiscleanup.core.common.utcTimeZone
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupAlertDialog
import com.crisiscleanup.core.designsystem.component.CrisisCleanupFilterChip
import com.crisiscleanup.core.designsystem.component.CrisisCleanupIconButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupRadioButton
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.component.DatePickerDialog
import com.crisiscleanup.core.designsystem.component.OutlinedSingleLineTextField
import com.crisiscleanup.core.designsystem.component.actionHeight
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons
import com.crisiscleanup.core.designsystem.theme.disabledAlpha
import com.crisiscleanup.core.designsystem.theme.listItemHorizontalPadding
import com.crisiscleanup.core.designsystem.theme.listItemNestedPadding
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.designsystem.theme.listItemVerticalPadding
import com.crisiscleanup.feature.caseeditor.weekdayOrderLookup
import com.philjay.Frequency
import com.philjay.RRule
import com.philjay.Weekday
import com.philjay.WeekdayNum
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.format.DateTimeFormatter

private val recurringDateFormat =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").utcTimeZone

private val rRuleWeekDays = listOf(
    Weekday.Monday,
    Weekday.Tuesday,
    Weekday.Wednesday,
    Weekday.Thursday,
    Weekday.Friday,
)

private val rRuleWeekDaysNums = rRuleWeekDays.map { WeekdayNum(0, it) }

private val weekdayTranslationLookup = mapOf(
    Weekday.Sunday to "recurringSchedule.sun",
    Weekday.Monday to "recurringSchedule.mon",
    Weekday.Tuesday to "recurringSchedule.tue",
    Weekday.Wednesday to "recurringSchedule.wed",
    Weekday.Thursday to "recurringSchedule.thu",
    Weekday.Friday to "recurringSchedule.fri",
    Weekday.Saturday to "recurringSchedule.sat",
)

@Composable
private fun RowScope.FrequencyOption(
    isSelected: Boolean,
    onSelected: () -> Unit,
    label: String,
    enabled: Boolean,
) {
    CrisisCleanupFilterChip(
        isSelected,
        {
            if (it) {
                onSelected()
            }
        },
        Modifier
            .weight(1f)
            .actionHeight(),
        label = {
            Text(
                label,
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        enabled = enabled,
    )
}

@Composable
private fun FrequencyDailyOption(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = false,
    onSelect: () -> Unit = {},
    text: String = "",
    trailingContent: (@Composable () -> Unit)? = null,
) {
    CrisisCleanupRadioButton(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = onSelect,
            )
            .then(modifier),
        selected = isSelected,
        onSelect = onSelect,
        enabled = enabled,
        text = text,
        trailingContent = trailingContent,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FrequencyDailyWeeklyViews(
    modifier: Modifier,
    rRuleIn: String,
    defaultRrule: String,
    enabled: Boolean,
    updateFrequency: (String) -> Unit = {},
) {
    val translator = LocalAppTranslator.current
    val rRuleString = rRuleIn.ifEmpty { defaultRrule }
    // TODO Test invalid string. Possibly parse manually if invalid?
    val rRule = RRule(rRuleString)

    var intervalAmount by remember { mutableStateOf(0) }
    val hideIntervalDialog = { intervalAmount = 0 }

    val updateRrule = {
        val updatedRrule = listOf(
            rRule.toRFC5545String(),
            "BYHOUR=11",
        )
            .filterNotBlankTrim()
            .joinToString(";")
        updateFrequency(updatedRrule)
    }
    val setRruleWeekdays = { byDays: Collection<WeekdayNum> ->
        rRule.byDay.clear()
        rRule.byDay.addAll(byDays)
    }

    val isDaily = rRule.freq == Frequency.Daily
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = listItemSpacedByHalf,
    ) {
        FrequencyOption(
            isDaily,
            {
                rRule.freq = Frequency.Daily
                if (rRule.byDay.isNotEmpty()) {
                    setRruleWeekdays(rRuleWeekDaysNums)
                }
                updateRrule()
            },
            translator("dashboard.daily"),
            enabled,
        )
        FrequencyOption(
            !isDaily,
            {
                rRule.freq = Frequency.Weekly
                if (rRule.byDay.isEmpty()) {
                    rRule.byDay.add(WeekdayNum(0, Weekday.Sunday))
                }
                updateRrule()
            },
            translator("dashboard.weekly"),
            enabled,
        )
    }
    if (isDaily) {
        val isEveryWeekday = rRule.byDay.isNotEmpty()
        val isEveryDay = !isEveryWeekday
        Column {
            FrequencyDailyOption(
                modifier,
                isEveryDay,
                enabled,
                {
                    rRule.byDay.clear()
                    updateRrule()
                },
            ) {
                Text(translator("recurringSchedule.recur_every"))
                FrequencyIntervalButton(
                    enabled && isEveryDay,
                    "${rRule.interval}",
                ) {
                    intervalAmount = rRule.interval.coerceAtLeast(1)
                }
                Text(translator("recurringSchedule.day_s"))
            }
            FrequencyDailyOption(
                modifier,
                isEveryWeekday,
                enabled,
                {
                    setRruleWeekdays(rRuleWeekDaysNums)
                    updateRrule()
                },
                translator("recurringSchedule.every_weekday"),
            )
        }
    } else {
        val nestedModifier = modifier.listItemNestedPadding()
        Row(
            modifier = nestedModifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(translator("recurringSchedule.recur_every"))
            FrequencyIntervalButton(
                enabled,
                "${rRule.interval}",
            ) {
                intervalAmount = rRule.interval.coerceAtLeast(1)
            }
            Text(translator("recurringSchedule.weeks_on"))
        }
        FlowRow(
            modifier = nestedModifier,
            horizontalArrangement = listItemSpacedBy,
        ) {
            val daySelectMap = remember {
                mutableStateMapOf<Weekday, Boolean>()
                    .also {
                        rRule.byDay.forEach { weekdayNum ->
                            it[weekdayNum.weekday] = true
                        }
                    }
            }
            weekdayTranslationLookup.forEach { (day, translationKey) ->
                val wasSelected = daySelectMap.contains(day)
                CrisisCleanupFilterChip(
                    selected = wasSelected,
                    onClick = { isToggleSelect ->
                        if (daySelectMap.size > 1 || isToggleSelect) {
                            if (isToggleSelect) {
                                daySelectMap[day] = true
                            } else {
                                daySelectMap.remove(day)
                            }

                            val selectedOptions = daySelectMap
                                .map { option -> WeekdayNum(0, option.key) }
                                .sortedBy { option -> weekdayOrderLookup[option.weekday] }
                            setRruleWeekdays(selectedOptions)
                            updateRrule()
                        }
                    },
                    label = { Text(translator(translationKey)) },
                    enabled = enabled,
                )
            }
        }
    }

    if (intervalAmount > 0) {
        FrequencyIntervalDialog(
            intervalAmount,
            { interval: Int? ->
                interval?.let {
                    rRule.interval = it.coerceAtLeast(1)
                    updateRrule()
                }
                hideIntervalDialog()
            },
            positiveActionText = translator("actions.save"),
            negativeActionText = translator("actions.cancel"),
        )
    }

    FrequencyDatePicker(enabled, translator, modifier, rRule, updateRrule)
}

@Composable
private fun FrequencyIntervalButton(
    enabled: Boolean,
    text: String,
    onClick: () -> Unit,
) = CrisisCleanupTextButton(
    Modifier.listItemHorizontalPadding(),
    onClick = onClick,
    enabled = enabled,
    text = text,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ),
    elevation = ButtonDefaults.buttonElevation(
        // TODO Common dimension
        defaultElevation = 1.dp,
    ),
)

@Composable
private fun FrequencyIntervalDialog(
    intervalIn: Int,
    closeDialog: (Int?) -> Unit,
    positiveActionText: String = "",
    negativeActionText: String = "",
) {
    var interval by remember { mutableStateOf(intervalIn.coerceAtLeast(1)) }
    var intervalText by remember { mutableStateOf("$interval") }
    val dismissDialog = { closeDialog(null) }
    val submitInterval = { closeDialog(interval) }

    CrisisCleanupAlertDialog(
        title = LocalAppTranslator.current("recurringSchedule.specify_interval"),
        onDismissRequest = dismissDialog,
        dismissButton = {
            CrisisCleanupTextButton(
                text = negativeActionText,
                onClick = dismissDialog,
            )
        },
        confirmButton = {
            CrisisCleanupTextButton(
                text = positiveActionText,
                onClick = submitInterval,
            )
        },
    ) {
        Row(
            Modifier.listItemVerticalPadding(),
            horizontalArrangement = listItemSpacedByHalf,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Minus,
                onClick = {
                    interval = (interval - 1).coerceAtLeast(1)
                    intervalText = "$interval"
                },
                enabled = interval > 1,
            )
            OutlinedSingleLineTextField(
                modifier = Modifier.sizeIn(minWidth = 36.dp, maxWidth = 64.dp),
                value = intervalText,
                onValueChange = { text: String ->
                    if (text.isBlank()) {
                        interval = 1
                        intervalText = ""
                    } else {
                        try {
                            interval = text.toInt().coerceAtLeast(1)
                            intervalText = "$interval"
                        } catch (e: Exception) {
                            Log.e("form-data-frequency-input", e.message, e)
                        }
                    }
                },
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onEnter = submitInterval,
                enabled = true,
                isError = false,
                labelResId = 0,
            )
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Add,
                onClick = {
                    interval++
                    intervalText = "$interval"
                },
            )
        }
    }
}

@Composable
private fun FrequencyDatePicker(
    enabled: Boolean,
    translator: KeyResourceTranslator,
    modifier: Modifier,
    rRule: RRule,
    updateRrule: () -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Row(
        Modifier
            .clickable(
                enabled = enabled,
                onClick = {
                    showDatePicker = true
                },
            )
            .then(modifier),
        horizontalArrangement = listItemSpacedBy,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectedDateText = translator("recurringSchedule.select_end_date")
        var tint = LocalContentColor.current
        if (!enabled) {
            tint = tint.disabledAlpha()
        }
        Icon(
            imageVector = CrisisCleanupIcons.Calendar,
            contentDescription = selectedDateText,
            tint = tint,
        )
        val dateText = rRule.until?.let {
            "$selectedDateText (${recurringDateFormat.format(it)})"
        } ?: selectedDateText
        Text(dateText)
        Spacer(
            Modifier
                .weight(1f)
                .actionHeight(),
        )
        rRule.until?.let {
            CrisisCleanupIconButton(
                imageVector = CrisisCleanupIcons.Clear,
                contentDescription = LocalAppTranslator.current("recurringSchedule.remove_until_recurring_date"),
                onClick = {
                    rRule.until = null
                    updateRrule()
                },
                enabled = enabled,
            )
        }
    }
    if (showDatePicker) {
        DatePickerDialog(
            selectedMillis = rRule.until?.toEpochMilli(),
            onCloseDialog = { millis ->
                millis?.let {
                    rRule.until = Instant.fromEpochMilliseconds(it).toJavaInstant()
                }
                updateRrule()
                showDatePicker = false
            },
        )
    }
}
