package com.crisiscleanup.core.common

import android.content.Context
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputValidatorTest {
    @MockK
    lateinit var context: Context
    private lateinit var inputValidator: InputValidator

    @Before
    fun setup() {
        inputValidator = CommonInputValidator(context)
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
