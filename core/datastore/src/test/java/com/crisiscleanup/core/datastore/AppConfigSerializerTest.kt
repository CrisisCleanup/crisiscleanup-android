package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class AppConfigSerializerTest {
    private val serializer = AppConfigSerializer()

    @Test
    fun defaultAppConfig_isEmpty() {
        assertEquals(
            appConfig {
                // Default value
            },
            serializer.defaultValue,
        )
    }

    @Test(expected = CorruptionException::class)
    fun readingInvalidAppConfig_throwsCorruptionException() = runTest {
        serializer.readFrom(ByteArrayInputStream(byteArrayOf(0)))
    }
}
