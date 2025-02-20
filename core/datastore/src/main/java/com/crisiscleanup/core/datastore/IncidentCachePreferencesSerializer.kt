package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class IncidentCachePreferencesSerializer @Inject constructor() :
    Serializer<IncidentCachePreferences> {
    override val defaultValue: IncidentCachePreferences =
        IncidentCachePreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): IncidentCachePreferences =
        try {
            // readFrom is already called on the data store background thread
            IncidentCachePreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: IncidentCachePreferences, output: OutputStream) {
        // writeTo is already called on the data store background thread
        t.writeTo(output)
    }
}
