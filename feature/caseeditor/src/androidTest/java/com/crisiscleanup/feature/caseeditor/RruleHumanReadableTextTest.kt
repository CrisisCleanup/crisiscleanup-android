package com.crisiscleanup.feature.caseeditor

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.crisiscleanup.core.common.AndroidResourceProvider
import com.crisiscleanup.core.common.ApplicationResourceProvider
import com.philjay.Frequency
import com.philjay.RRule
import com.philjay.Weekday
import com.philjay.WeekdayNum
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RruleHumanReadableTextTest {
    private lateinit var resourceProvider: AndroidResourceProvider

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        resourceProvider = ApplicationResourceProvider(appContext)
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
            ).toHumanReadableText(resourceProvider)
            val expected = ""
            assertEquals(expected, actual)
        }
    }

    @Test
    fun dailyEveryDay() {
        val actual = makeRrule(Frequency.Daily).toHumanReadableText(resourceProvider)
        val expected = "Every day."
        assertEquals(expected, actual)

        val actualUntil = makeRrule(
            Frequency.Daily,
            until = untilA,
        ).toHumanReadableText(resourceProvider)
        println("Until $untilADate")
        val expectedUntil = "Every day until $untilADate."
        assertEquals(expectedUntil, actualUntil)
    }

    @Test
    fun dailyEveryOneDay() {
        val actual = makeRrule(Frequency.Daily, 1).toHumanReadableText(resourceProvider)
        val expected = "Every day."
        assertEquals(expected, actual)
    }

    @Test
    fun dailyEveryNDays() {
        val actual = makeRrule(Frequency.Daily, 2).toHumanReadableText(resourceProvider)
        val expected = "Every 2 days."
        assertEquals(expected, actual)
    }

    @Test
    fun dailyEveryMtoF() {
        val actual = makeRrule(
            Frequency.Daily,
            2,
            listOf(Weekday.Sunday),
        ).toHumanReadableText(resourceProvider)
        val expected = "Every weekday (M-F)."
        assertEquals(expected, actual)
    }

    @Test
    fun weeklyNoDays() {
        val actual = makeRrule(
            Frequency.Weekly,
        ).toHumanReadableText(resourceProvider)
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
        ).toHumanReadableText(resourceProvider)
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
        ).toHumanReadableText(resourceProvider)
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
        ).toHumanReadableText(resourceProvider)
        val expectedA = "Every 3 weeks on Wednesday, Thursday, and Friday."
        assertEquals(expectedA, actualA)

        val actualB = makeRrule(
            Frequency.Weekly,
            6,
            listOf(
                Weekday.Wednesday,
            ),
        ).toHumanReadableText(resourceProvider)
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
        ).toHumanReadableText(resourceProvider)
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
        ).toHumanReadableText(resourceProvider)
        val expectedD = "Every 6 weeks on Sunday and Wednesday."
        assertEquals(expectedD, actualD)
    }
}