package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class AppMaintenanceSerializer @Inject constructor() : Serializer<AppMaintenance> {
    override val defaultValue: AppMaintenance = AppMaintenance.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AppMaintenance =
        try {
            // readFrom is already called on the data store background thread
            AppMaintenance.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: AppMaintenance, output: OutputStream) {
        // writeTo is already called on the data store background thread
        t.writeTo(output)
    }
}
