package com.crisiscleanup.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class CasesFiltersProtoSerializer @Inject constructor() : Serializer<LocalPersistedCasesFilters> {
    override val defaultValue: LocalPersistedCasesFilters =
        LocalPersistedCasesFilters.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LocalPersistedCasesFilters =
        try {
            // readFrom is already called on the data store background thread
            LocalPersistedCasesFilters.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }

    override suspend fun writeTo(t: LocalPersistedCasesFilters, output: OutputStream) {
        // writeTo is already called on the data store background thread
        t.writeTo(output)
    }
}
