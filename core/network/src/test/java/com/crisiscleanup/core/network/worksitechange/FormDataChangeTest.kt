package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.network.model.DynamicValue
import com.crisiscleanup.core.network.model.KeyDynamicValuePair
import org.junit.Test
import kotlin.test.assertEquals

class FormDataChangeTest {
    private val emptyFormData = listOf<KeyDynamicValuePair>()
    private val baseFormData = listOf(
        KeyDynamicValuePair("abf", DynamicValue("", true)),
        KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, true)),
        KeyDynamicValuePair("bs", DynamicValue("botu")),
        KeyDynamicValuePair("cs", DynamicValue("cosu")),
        KeyDynamicValuePair("ds", DynamicValue("domu")),
        KeyDynamicValuePair("es", DynamicValue("eopu")),
    )

    private val emptyWorksite = testNetworkWorksite(formData = emptyFormData)
    private val baseWorksite = testNetworkWorksite(formData = baseFormData)

    @Test
    fun noChange() {
        assertEquals(emptyFormData, emptyWorksite.getFormDataChanges(emptyMap(), emptyMap()))
        assertEquals(baseFormData, baseWorksite.getFormDataChanges(emptyMap(), emptyMap()))

        val noChangeMap = mapOf(
            "a" to DynamicValue("b"),
            "b" to DynamicValue("", isBoolean = true, true),
        )
        assertEquals(emptyFormData, emptyWorksite.getFormDataChanges(noChangeMap, noChangeMap))
        assertEquals(baseFormData, baseWorksite.getFormDataChanges(noChangeMap, noChangeMap))
    }

    @Test
    fun newAdd() {
        val newChangesMap = mapOf(
            "new-a" to DynamicValue("b"),
            "new-b" to DynamicValue("", isBoolean = true, true),
        )

        val expectedEmpty = newChangesMap.map { KeyDynamicValuePair(it.key, it.value) }
        assertEquals(expectedEmpty, emptyWorksite.getFormDataChanges(emptyMap(), newChangesMap))

        val expectedBase = listOf(
            KeyDynamicValuePair("abf", DynamicValue("", true)),
            KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, true)),
            KeyDynamicValuePair("bs", DynamicValue("botu")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("ds", DynamicValue("domu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
            KeyDynamicValuePair("new-a", DynamicValue("b")),
            KeyDynamicValuePair("new-b", DynamicValue("", isBoolean = true, true)),
        )
        assertEquals(expectedBase, baseWorksite.getFormDataChanges(emptyMap(), newChangesMap))
    }

    @Test
    fun newUpdate() {
        val newChangesMap = mapOf(
            "bs" to DynamicValue("bs-new"),
            "abf" to DynamicValue("", isBoolean = true, true),
        )

        val expected = listOf(
            KeyDynamicValuePair("abf", DynamicValue("", isBoolean = true, true)),
            KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, true)),
            KeyDynamicValuePair("bs", DynamicValue("bs-new")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("ds", DynamicValue("domu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
        )

        assertEquals(expected, baseWorksite.getFormDataChanges(emptyMap(), newChangesMap))
    }

    @Test
    fun deleteNone() {
        val changesMap = mapOf(
            "r" to DynamicValue("r"),
            "s" to DynamicValue("s"),
        )

        assertEquals(emptyFormData, emptyWorksite.getFormDataChanges(changesMap, emptyMap()))
        assertEquals(baseFormData, baseWorksite.getFormDataChanges(changesMap, emptyMap()))
    }

    @Test
    fun deleteExisting() {
        val fromMap = mapOf(
            "bs" to DynamicValue("botu"),
            "ds" to DynamicValue("domu"),
            "abt" to DynamicValue("", isBoolean = true, true),
            "r" to DynamicValue("", isBoolean = true, true),
        )
        val toMap = mapOf(
            "bs" to DynamicValue("botu"),
        )

        val actual = baseWorksite.getFormDataChanges(fromMap, toMap)

        val expected = listOf(
            KeyDynamicValuePair("abf", DynamicValue("", true)),
            KeyDynamicValuePair("bs", DynamicValue("botu")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun changeNew() {
        val start = mapOf(
            "change-new" to DynamicValue("", isBoolean = true, true),
        )
        val change = mapOf(
            "change-new" to DynamicValue("", isBoolean = true, false),
        )

        val actual = baseWorksite.getFormDataChanges(start, change)

        val expected = listOf(
            KeyDynamicValuePair("abf", DynamicValue("", true)),
            KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, true)),
            KeyDynamicValuePair("bs", DynamicValue("botu")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("ds", DynamicValue("domu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
            KeyDynamicValuePair("change-new", DynamicValue("", true)),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun changeExisting() {
        val start = mapOf(
            "abt" to DynamicValue("", isBoolean = true, true),
            "cs" to DynamicValue("c-unchanged"),
            "bs" to DynamicValue("botu-a"),
        )
        val change = mapOf(
            "abt" to DynamicValue("", isBoolean = true, false),
            "cs" to DynamicValue("c-unchanged"),
            "bs" to DynamicValue("botu-b"),
        )

        val actual = baseWorksite.getFormDataChanges(start, change)

        val expected = listOf(
            KeyDynamicValuePair("abf", DynamicValue("", true)),
            KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, false)),
            KeyDynamicValuePair("bs", DynamicValue("botu-b")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("ds", DynamicValue("domu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun unchanged() {
        val start = mapOf(
            "abt" to DynamicValue("", isBoolean = true, true),
            "cs" to DynamicValue("c-unchanged"),
            "unchanged" to DynamicValue("unchanged"),
        )
        val change = mapOf(
            "abt" to DynamicValue("", isBoolean = true, true),
            "cs" to DynamicValue("c-unchanged"),
            "unchanged" to DynamicValue("unchanged"),
        )

        assertEquals(emptyList(), emptyWorksite.getFormDataChanges(start, change))

        val actual = baseWorksite.getFormDataChanges(start, change)
        val expected = listOf(
            KeyDynamicValuePair("abf", DynamicValue("", true)),
            KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, true)),
            KeyDynamicValuePair("bs", DynamicValue("botu")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("ds", DynamicValue("domu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
        )
        assertEquals(expected, actual)
    }

    @Test
    fun complex() {
        val start = mapOf(
            "unchanged" to DynamicValue("unchanged"),
            "ds" to DynamicValue("ds-no-change"),
            "bs" to DynamicValue("start-bs"),
            "peno" to DynamicValue("din"),
            "start-dis" to DynamicValue("is deleting"),
            "abf" to DynamicValue("", isBoolean = true, true),
            "arto" to DynamicValue("starto"),
        )
        val change = mapOf(
            "new" to DynamicValue("", isBoolean = true, true),
            "abt" to DynamicValue("", isBoolean = true, false),
            "unchanged" to DynamicValue("unchanged"),
            "ds" to DynamicValue("ds-no-change"),
            "bs" to DynamicValue("change-bs"),
            "peno" to DynamicValue("fin"),
            "arto" to DynamicValue("starto"),
        )

        val actual = baseWorksite.getFormDataChanges(start, change)
        val expected = listOf(
            KeyDynamicValuePair("abt", DynamicValue("", isBoolean = true, false)),
            KeyDynamicValuePair("bs", DynamicValue("change-bs")),
            KeyDynamicValuePair("cs", DynamicValue("cosu")),
            KeyDynamicValuePair("ds", DynamicValue("domu")),
            KeyDynamicValuePair("es", DynamicValue("eopu")),
            KeyDynamicValuePair("new", DynamicValue("", isBoolean = true, true)),
            KeyDynamicValuePair("peno", DynamicValue("fin")),
        )
        assertEquals(expected, actual)
    }
}
