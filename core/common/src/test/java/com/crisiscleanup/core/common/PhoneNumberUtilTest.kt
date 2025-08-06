package com.crisiscleanup.core.common

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumberUtilTest {
    @Test
    fun invalidPhoneNumbers() {
        val inputs = listOf(
            "",
            "   ",
            " 12345 ",
            " no numbers",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertNull(actual)
        }
    }

    @Test
    fun tenDigitNumberExactly() {
        val inputs = listOf(
            "1234567890",
            "  1234567890",
            "1234567890   ",
            "  1234567890  ",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("1234567890"), actual)
        }
    }

    @Test
    fun noCompaction() {
        val inputs = listOf(
            " (234)5678901 ",
            "(234)5678901",
            " 1(234)5678901",
            "12345678901 ",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("2345678901"), actual)
        }
    }

    @Test
    fun commonFormats() {
        val inputs = listOf(
            "(234) 567-8901",
            "(234) 567.8901",
            "(234) 567 8901",
            "1(234) 567-8901",
            "1(234) 567.8901",
            "1 (234) 567 8901",
            "+1(234) 567-8901",
            "+1 (234) 567.8901",
            "+1 (234) 567 8901",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("2345678901"), actual)
        }
    }

    @Test
    fun compact334() {
        val inputs = listOf(
            "234 567 8901",
            "234-567-8901",
            "234.567.8901",
            "1234 567 8901",
            "1234-567-8901",
            "1234.567.8901",
            "1234.567.8901",
            "(234).567.8901",
            "1(234).567.8901",
            "234 567  8901 ",
            " 234  567 8901",
            " 234  567  8901 ",
            "1234  567  8901 ",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("2345678901"), actual)
        }
    }

    @Test
    fun dashParenthesis() {
        val inputs = listOf(
            "234567-8901",
            "234-5678901",
            "234) 567-8901",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("2345678901"), actual)
        }
    }

    @Test
    fun nonNumeric3764() {
        val inputs = listOf(
            "234 5678901",
            "1234 5678901",
            "something 1234 5678901-cell",
            "+234 5678901 (air)",
            "2345678901 a number",
            "a 1234 5678901 b",
            "234567 8901",
            "something 234567 8901-cell",
            "a 234567 8901 b",
            "1234567 8901",
            "12345678901 for anyone",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("2345678901"), actual)
        }
    }

    @Test
    fun possibleMultiple() {
        val inputs = mapOf(
            "23456789012" to listOf("23456789012"),
            "1\u202A2345678901" to listOf("2345678901"),
            "2345678901 or 3456789012" to listOf(
                "2345678901",
                "3456789012",
            ),
            "234567890" to listOf("234567890"),
            "234567890-" to listOf("234567890"),
            "2345678901  . 4282 M-F" to listOf("2345678901"),
            "2345678901 / 3456789012" to listOf(
                "2345678901",
                "3456789012",
            ),
            "1.7068339198" to listOf("7068339198"),
            "(23456789012" to listOf("23456789012"),
            "18002345678901" to listOf("8002345678901"),
            "2345678901   1st" to listOf("2345678901"),
            "2345678901             9  0" to listOf("2345678901"),
            "2345678901 (  be 3456789012)" to listOf(
                "2345678901",
                "3456789012",
            ),
            "2345678901/3456789012" to listOf(
                "2345678901",
                "3456789012",
            ),
            "  2345678901 or      3456789012 " to listOf(
                "2345678901",
                "3456789012",
            ),
            "2345678901/ 3456789012 ( )" to listOf(
                "2345678901",
                "3456789012",
            ),
            "2345678901/3456789012/4567891234" to listOf(
                "2345678901",
                "3456789012",
                "4567891234",
            ),
            "2345678901, 3456789012, 4567891234 \n" to listOf(
                "2345678901",
                "3456789012",
                "4567891234",
            ),
            "1234567890r9 " to listOf("234567890"),
            "2345678901 x558   " to listOf("2345678901"),
        )
        for ((input, expected) in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(expected, actual, input)
        }
    }

    @Test
    fun dotDelimited11() {
        val inputs = listOf(
            "234.5678901",
            "(234.5678901",
        )
        for (input in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers
            assertEquals(listOf("2345678901"), actual)
        }
    }

    @Test
    fun mostlyNumbers() {
        val inputs = mapOf(
            "234 567- 8901-  " to "2345678901",
            "1234*5678901" to "12345678901",
            "123 4567 8901" to "12345678901",
            "123456 78901" to "12345678901",
        )
        for ((input, expected) in inputs) {
            val actual = PhoneNumberUtil.parsePhoneNumbers(input)?.parsedNumbers?.first()
            assertEquals(expected, actual, input)
        }
    }
}
