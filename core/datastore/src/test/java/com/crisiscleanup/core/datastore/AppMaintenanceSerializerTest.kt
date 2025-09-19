package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class AppMaintenanceSerializerTest {
    private val serializer = AppMaintenanceSerializer()

    @Test
    fun defaultAppMaintenance_isEmpty() {
        assertEquals(
            appMaintenance {
                // Default value
            },
            serializer.defaultValue,
        )
    }

    @Test(expected = CorruptionException::class)
    fun readingInvalidAppMaintenance_throwsCorruptionException() = runTest {
        serializer.readFrom(ByteArrayInputStream(byteArrayOf(0)))
    }
}
