package com.crisiscleanup.core.common

import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputValidatorTest {
    private lateinit var inputValidator: InputValidator

    @Before
    fun setup() {
        inputValidator = CommonInputValidator()
    }

    @Test
    fun hasEmailAddress() {
        val notEmailAddresses = listOf(
            "a sentence is worth 30 works",
            "a@b",
            "a@b.c",
            "url.org",
            "https://www.crisiscleanup.org",
        )
        for (s in notEmailAddresses) {
            assertFalse(inputValidator.hasEmailAddress(s))
        }

        val emailAddresses = listOf(
            "a@b.cd",
            "o35@gs8sdf.rifg",
            "one@person.net or ther@e.er",
        )
        for (s in emailAddresses) {
            assertTrue(inputValidator.hasEmailAddress(s))
        }
    }
}