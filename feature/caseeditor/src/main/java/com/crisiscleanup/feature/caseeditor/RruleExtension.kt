package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.combineTrimText
import com.philjay.Frequency
import com.philjay.RRule
import com.philjay.Weekday
import com.philjay.WeekdayNum
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val weekdayOrderLookup = mapOf(
    Weekday.Sunday to 0,
    Weekday.Monday to 1,
    Weekday.Tuesday to 2,
    Weekday.Wednesday to 3,
    Weekday.Thursday to 4,
    Weekday.Friday to 5,
    Weekday.Saturday to 6,
)

data class RruleProfile(
    /**
     * M-F
     */
    val isWeekdays: Boolean,
    val isAllDays: Boolean,
)

fun RRule.profile(): RruleProfile {
    val days = byDay.map(WeekdayNum::weekday).toSet()
    val hasWeekDays = days.contains(Weekday.Monday) &&
            days.contains(Weekday.Tuesday) &&
            days.contains(Weekday.Wednesday) &&
            days.contains(Weekday.Thursday) &&
            days.contains(Weekday.Friday)
    val isWeekdays = hasWeekDays &&
            !days.contains(Weekday.Saturday) &&
            !days.contains(Weekday.Sunday)
    val isEveryDay = hasWeekDays &&
            !isWeekdays &&
            days.contains(Weekday.Saturday) &&
            days.contains(Weekday.Sunday)
    return RruleProfile(isWeekdays, isEveryDay)
}

private val untilDateFormat = DateTimeFormatter
    .ofPattern("yyyy MMM d")
    .withZone(ZoneId.systemDefault())

// Tested in [RruleHumanReadableTextTest]
fun RRule.toHumanReadableText(
    resourceProvider: AndroidResourceProvider,
): String {
    val resources = resourceProvider.resources

    if (freq == Frequency.Daily || freq == Frequency.Weekly) {
        val positiveInterval = interval.coerceAtLeast(1)
        val frequencyPart = when (freq) {
            Frequency.Daily -> {
                if (byDay.isEmpty()) {
                    resources.getQuantityString(
                        R.plurals.n_days,
//I have no idea how to add the translations, so here they are: recurringSchedule.n_days_one, recurringSchedule.n_days_other
                        positiveInterval,
                        positiveInterval,
                    )
                } else {
                    translator("recurringSchedule.weekday_mtof")
                }
            }

            Frequency.Weekly -> {
                if (byDay.isNotEmpty()) {
                    var weekPart = resources.getQuantityString(
                        R.plurals.n_weeks,
//I have no idea how to add the translations, so here they are: recurringSchedule.n_weeks_one, recurringSchedule.n_weeks_other
                        positiveInterval,
                        positiveInterval,
                    )
                    val profile = profile()
                    if (profile.isAllDays) {
                        val everyDay = translator("recurringSchedule.every_day")
                        weekPart = "$weekPart $everyDay"
                    } else if (profile.isWeekdays) {
                        val onWeekdays = translator("recurringSchedule.on_weekdays")
                        weekPart = "$weekPart $onWeekdays"
                    } else {
                        val sundayToSaturday =
                            resources.getStringArray(R.array.days_sunday_through_saturday)
//I have no idea how to add the translations, so here they are: recurringSchedule.sunday, recurringSchedule.monday, recurringSchedule.tuesday, recurringSchedule.wednesday, recurringSchedule.thursday, recurringSchedule.friday, recurringSchedule.saturday
                        val sortedDays = byDay.map(WeekdayNum::weekday)
                            .toSet()
                            .asSequence()
                            .sortedBy { weekdayOrderLookup[it] }
                            .map {
                                weekdayOrderLookup[it]?.let { dayIndex ->
                                    if (dayIndex in sundayToSaturday.indices) {
                                        sundayToSaturday[dayIndex]
                                    } else {
                                        ""
                                    }
                                } ?: ""
                            }
                            .filter(String::isNotBlank)
                            .toList()
                        val onDays = if (sortedDays.size == 1) {
                            val daysString = sortedDays.joinToString(", ")
                            translator("recurringSchedule.on_days", daysString)
                        } else if (sortedDays.size > 1) {
                            val startDays = sortedDays.slice(0 until sortedDays.size - 1)
                            val daysString = startDays.joinToString(", ")
                            resources.getQuantityString(
                                R.plurals.on_and_days,
//I have no idea how to add the translations, so here they are: recurringSchedule.on_and_days_one, recurringSchedule.on_and_days_other
                                startDays.size,
                                daysString,
                                sortedDays.last(),
                            )
                        } else {
                            ""
                        }
                        if (onDays.isNotEmpty()) {
                            weekPart = "$weekPart $onDays"
                        }
                    }
                    weekPart
                } else {
                    ""
                }
            }

            else -> ""
        }
        if (frequencyPart.isNotBlank()) {
            val every = translator("recurringSchedule.every")
            val untilDate = until?.let {
                untilDateFormat.format(it)
            }
            val untilPart = if (untilDate?.isNotBlank() == true) resources.getString(
               translator("recurringSchedule.until_date", untilDate)
            ) else ""
            val frequencyString = listOf(
                every,
                frequencyPart,
                untilPart,
            ).combineTrimText(" ")
            return "$frequencyString."
        }
    }
    return ""
}
