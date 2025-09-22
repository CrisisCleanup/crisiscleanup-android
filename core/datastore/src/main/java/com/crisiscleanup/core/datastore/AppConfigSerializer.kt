package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class AppConfigSerializer @Inject constructor() : Serializer<AppConfig> {
    override val defaultValue: AppConfig = AppConfig.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppConfig =
        try {
            // readFrom is already called on the data store background thread
            AppConfig.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: AppConfig, output: OutputStream) {
        // writeTo is already called on the data store background thread
        t.writeTo(output)
    }
}
