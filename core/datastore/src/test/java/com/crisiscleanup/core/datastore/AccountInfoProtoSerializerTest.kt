package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

class AccountInfoProtoSerializerTest {
    private val serializer = AccountInfoProtoSerializer()

    @Test
    fun default_isEmpty() {
        assertEquals(
            accountInfo {
                // Default value
            },
            serializer.defaultValue
        )
    }

    @Test(expected = CorruptionException::class)
    fun readingInvalidAccountInfo_throwsCorruptionException() = runTest {
        serializer.readFrom(ByteArrayInputStream(byteArrayOf(0)))
    }
}