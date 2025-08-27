package com.mss.thebigcalendar.data.model.proto

import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream

object DeletedActivitySerializer : Serializer<TrashActivities> {
    override val defaultValue: TrashActivities = TrashActivities.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TrashActivities {
        return TrashActivities.parseFrom(input)
    }

    override suspend fun writeTo(t: TrashActivities, output: OutputStream) {
        t.writeTo(output)
    }
}
