package com.crisiscleanup.core.network.worksitechange

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringChangeTest {
    @Test
    fun changes() {
        // No diff (between null, empty, blank) results in base
        assertEquals("base", "base".change("same", "same"))
        assertEquals("base", "base".change("same ", " same"))

        // Diff results in to
        assertEquals("to", "base".change("", "to"))
        assertEquals("to", "base".change(" ", "to"))
        assertEquals("to", "base".change("from", "to"))
        assertEquals("", "base".change("from", ""))
        assertEquals(" ", "base".change("from", " "))
    }

    @Test
    fun baseChange_nullBase() {
        // No diff (between null, empty, blank) results in base (null)
        assertNull(baseChange(null, null, null))
        assertNull(baseChange(null, "", null))
        assertNull(baseChange(null, null, ""))
        assertNull(baseChange(null, " ", null))
        assertNull(baseChange(null, null, " "))
        assertNull(baseChange(null, "same", "same"))
        assertNull(baseChange(null, " same", "same "))

        // Diff results in to
        assertEquals("to", baseChange(null, null, "to"))
        assertEquals("to", baseChange(null, "", "to"))
        assertEquals("to", baseChange(null, " ", "to"))
        assertEquals("to", baseChange(null, "from", "to"))
        assertEquals(null, baseChange(null, "from", null))
        assertEquals("", baseChange(null, "from", ""))
        assertEquals(" ", baseChange(null, "from", " "))
    }

    @Test
    fun baseChange_baseNotNull() {
        // No diff (between null, empty, blank) results in base
        assertEquals("base", baseChange("base", null, null))
        assertEquals("base", baseChange("base", "", null))
        assertEquals("base", baseChange("base", null, ""))
        assertEquals("base", baseChange("base", " ", null))
        assertEquals("base", baseChange("base", null, " "))
        assertEquals("base", baseChange("base", "same", "same"))
        assertEquals("base", baseChange("base", "same ", " same"))

        // Diff results in to
        assertEquals("to", baseChange("base", null, "to"))
        assertEquals("to", baseChange("base", "", "to"))
        assertEquals("to", baseChange("base", " ", "to"))
        assertEquals("to", baseChange("base", "from", "to"))
        assertEquals(null, baseChange("base", "from", null))
        assertEquals("", baseChange("base", "from", ""))
        assertEquals(" ", baseChange("base", "from", " "))
    }
}
