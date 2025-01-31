package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class IncidentCachePreferencesSerializerTest {
    private val serializer = IncidentCachePreferencesSerializer()

    @Test
    fun defaultCachePreferences_isEmpty() {
        assertEquals(
            incidentCachePreferences {
                // Default value
            },
            serializer.defaultValue,
        )
    }

    @Test(expected = CorruptionException::class)
    fun readingInvalidCachePreferences_throwsCorruptionException() = runTest {
        serializer.readFrom(ByteArrayInputStream(byteArrayOf(0)))
    }
}
