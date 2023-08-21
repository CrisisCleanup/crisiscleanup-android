package com.crisiscleanup.core.database.model

import org.junit.Test
import kotlin.test.assertEquals

class CaseNumberOrderTest {
    @Test
    fun parseNoCaseNumber() {
        val noCaseNumbers = listOf(
            "",
            "leters",
            "symbols*F@",
        )

        noCaseNumbers.forEach {
            assertEquals(0, parseCaseNumberOrder(it))
        }
    }

    @Test
    fun parseCaseNumber() {
        val caseNumbers = listOf(
            Pair("35ksd", 35L),
            Pair("ef-5235", 5235L),
            Pair("Pre 642Post", 642L),
            Pair("a62b46", 62L),
        )

        caseNumbers.forEach {
            assertEquals(it.second, parseCaseNumberOrder(it.first))
        }
    }
}
