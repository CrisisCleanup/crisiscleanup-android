package com.crisiscleanup.core.common

import org.junit.Test
import kotlin.test.assertEquals

class PhoneNumberUtilTest {
    @Test
    fun emptyPhoneNumbers() {
        val actual = PhoneNumberUtil.getPhoneNumbers(listOf("", "  "))
        assertEquals(emptyList(), actual)
    }

    @Test
    fun tenElevenDigitPhoneNumbers() {
        val actual = PhoneNumberUtil.getPhoneNumbers(listOf("1234567890", "11234567890"))
        val expected = listOf(
            ParsedPhoneNumber("1234567890", listOf("1234567890")),
            ParsedPhoneNumber("11234567890", listOf("11234567890")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun commonSpacedPhoneNumbers() {
        val actual = PhoneNumberUtil.getPhoneNumbers(listOf("123 456 7890", "123 4567890"))
        val expected = listOf(
            ParsedPhoneNumber("123 456 7890", listOf("1234567890")),
            ParsedPhoneNumber("123 4567890", listOf("1234567890")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun doublePhoneNumber() {
        val actual = PhoneNumberUtil.getPhoneNumbers(listOf("1234567890dgk e*11234567890"))
        val expected = listOf(
            ParsedPhoneNumber("1234567890dgk e*11234567890", listOf("1234567890", "11234567890")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun onlyNumbers() {
        val actual = PhoneNumberUtil.getPhoneNumbers(listOf("1-2/3a4.5-6:7890"))
        val expected = listOf(
            ParsedPhoneNumber("1-2/3a4.5-6:7890", listOf("1234567890")),
        )
        assertEquals(expected, actual)
    }
}
