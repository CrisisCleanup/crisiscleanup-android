package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.common.KeyResourceTranslator
import com.philjay.Frequency
import com.philjay.RRule
import com.philjay.Weekday
import com.philjay.WeekdayNum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class RruleHumanReadableTextTest {
    private val translator = object : KeyResourceTranslator {
        override val translationCount: StateFlow<Int> = MutableStateFlow(0)

        override fun translate(phraseKey: String): String? {
            return when (phraseKey) {
                "recurringSchedule.n_days_one" -> "day"
                "recurringSchedule.n_days_other" -> "days"
                "recurringSchedule.weekday_mtof" -> "weekday (M-F)"
                "recurringSchedule.n_weeks_one" -> " week"
                "recurringSchedule.n_weeks_other" -> "weeks"
                "recurringSchedule.every_day" -> "every day"
                "recurringSchedule.on_weekdays" -> "on weekdays (M-F)"
                "recurringSchedule.sunday" -> "Sunday"
                "recurringSchedule.monday" -> "Monday"
                "recurringSchedule.tuesday" -> "Tuesday"
                "recurringSchedule.wednesday" -> "Wednesday"
                "recurringSchedule.thursday" -> "Thursday"
                "recurringSchedule.friday" -> "Friday"
                "recurringSchedule.saturday" -> "Saturday"
                "recurringSchedule.on_days" -> "on"
                "recurringSchedule.on_and_days_one" -> "on {days} and {last_day}"
                "recurringSchedule.on_and_days_other" -> "on {days}, and {last_day}"
                "recurringSchedule.every" -> "Every"
                "recurringSchedule.until_date" -> "until"
                else -> null
            }
        }

        override fun translate(phraseKey: String, fallbackResId: Int): String {
            return translate(phraseKey) ?: phraseKey
        }
    }

    private val untilA = Instant.fromEpochSeconds(2639649641)
    private val untilADate = "2053 Aug 24"

    private fun makeRrule(
        frequency: Frequency = Frequency.Weekly,
        interval: Int = 0,
        days: List<Weekday> = emptyList(),
        until: Instant? = null,
    ) = RRule().apply {
        freq = frequency
        this.interval = interval
        byDay.clear()
        byDay.addAll(days.map { WeekdayNum(0, it) })
        this.until = until?.toJavaInstant()
    }

    @Test
    fun notDailyWeeklyFrequency() {
        val frequencies = listOf(
            Frequency.Monthly,
            Frequency.Yearly,
        )
        frequencies.forEach {
            val actual = makeRrule(
                it,
                3,
                listOf(Weekday.Monday),
                untilA,
            ).toHumanReadableText(translator)
            val expected = ""
            assertEquals(expected, actual)
        }
    }

    @Test
    fun dailyEveryDay() {
        val actual = makeRrule(Frequency.Daily).toHumanReadableText(translator)
        val expected = "Every day."
        assertEquals(expected, actual)

        val actualUntil = makeRrule(
            Frequency.Daily,
            until = untilA,
        ).toHumanReadableText(translator)
        val expectedUntil = "Every day until $untilADate."
        assertEquals(expectedUntil, actualUntil)
    }

    @Test
    fun dailyEveryOneDay() {
        val actual = makeRrule(Frequency.Daily, 1).toHumanReadableText(translator)
        val expected = "Every day."
        assertEquals(expected, actual)
    }

    @Test
    fun dailyEveryNDays() {
        val actual = makeRrule(Frequency.Daily, 2).toHumanReadableText(translator)
        val expected = "Every 2 days."
        assertEquals(expected, actual)
    }

    @Test
    fun dailyEveryMtoF() {
        val actual = makeRrule(
            Frequency.Daily,
            2,
            listOf(Weekday.Sunday),
        ).toHumanReadableText(translator)
        val expected = "Every weekday (M-F)."
        assertEquals(expected, actual)
    }

    @Test
    fun weeklyNoDays() {
        val actual = makeRrule(
            Frequency.Weekly,
        ).toHumanReadableText(translator)
        val expected = ""
        assertEquals(expected, actual)
    }

    @Test
    fun weeklyAllDays() {
        val actual = makeRrule(
            Frequency.Weekly,
            2,
            listOf(
                Weekday.Monday,
                Weekday.Thursday,
                Weekday.Tuesday,
                Weekday.Wednesday,
                Weekday.Saturday,
                Weekday.Sunday,
                Weekday.Friday,
            )
        ).toHumanReadableText(translator)
        val expected = "Every 2 weeks every day."
        assertEquals(expected, actual)
    }

    @Test
    fun weeklyWeekdays() {
        val actual = makeRrule(
            Frequency.Weekly,
            1,
            listOf(
                Weekday.Monday,
                Weekday.Thursday,
                Weekday.Friday,
                Weekday.Tuesday,
                Weekday.Wednesday,
            )
        ).toHumanReadableText(translator)
        val expected = "Every week on weekdays (M-F)."
        assertEquals(expected, actual)
    }

    @Test
    fun weeklyCertainDays() {
        val actualA = makeRrule(
            Frequency.Weekly,
            3,
            listOf(
                Weekday.Thursday,
                Weekday.Friday,
                Weekday.Wednesday,
            ),
        ).toHumanReadableText(translator)
        val expectedA = "Every 3 weeks on Wednesday, Thursday, and Friday."
        assertEquals(expectedA, actualA)

        val actualB = makeRrule(
            Frequency.Weekly,
            6,
            listOf(
                Weekday.Wednesday,
            ),
        ).toHumanReadableText(translator)
        val expectedB = "Every 6 weeks on Wednesday."
        assertEquals(expectedB, actualB)

        val actualC = makeRrule(
            Frequency.Weekly,
            1,
            listOf(
                Weekday.Monday,
                Weekday.Thursday,
                Weekday.Wednesday,
                Weekday.Saturday,
                Weekday.Sunday,
                Weekday.Friday,
            ),
            untilA,
        ).toHumanReadableText(translator)
        val expectedC =
            "Every week on Sunday, Monday, Wednesday, Thursday, Friday, and Saturday until $untilADate."
        assertEquals(expectedC, actualC)

        val actualD = makeRrule(
            Frequency.Weekly,
            6,
            listOf(
                Weekday.Wednesday,
                Weekday.Sunday,
            ),
        ).toHumanReadableText(translator)
        val expectedD = "Every 6 weeks on Sunday and Wednesday."
        assertEquals(expectedD, actualD)
    }
}