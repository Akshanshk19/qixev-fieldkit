package com.qixev.qfieldkit.data.local.database

import androidx.room.TypeConverter
import com.qixev.qfieldkit.data.local.entity.JobStatus

class FieldKitConverters {

    @TypeConverter
    fun fromJobStatus(status: JobStatus): String {
        return status.name
    }

    @TypeConverter
    fun toJobStatus(value: String): JobStatus {
        return try {
            JobStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            JobStatus.RECEIVED
        }
    }
}

