package com.mss.thebigcalendar.data.repository

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.mss.thebigcalendar.data.model.proto.Activities
import java.io.InputStream
import java.io.OutputStream

object ActivitySerializer : Serializer<Activities> {
    override val defaultValue: Activities = Activities.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Activities {
        try {
            return Activities.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Activities, output: OutputStream) = t.writeTo(output)
}
