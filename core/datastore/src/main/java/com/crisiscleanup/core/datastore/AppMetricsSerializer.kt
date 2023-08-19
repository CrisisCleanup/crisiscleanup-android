package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class AppMetricsSerializer @Inject constructor() : Serializer<AppMetrics> {
    override val defaultValue: AppMetrics = AppMetrics.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppMetrics =
        try {
            // readFrom is already called on the data store background thread
            AppMetrics.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: AppMetrics, output: OutputStream) {
        // writeTo is already called on the data store background thread
        t.writeTo(output)
    }
}