package com.mudmap.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

@Database(
    entities = [TrailEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverters::class)
abstract class MudMapDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}

class StringListConverters {
    @TypeConverter
    fun encode(values: List<String>): String =
        JSONArray(values).toString()

    @TypeConverter
    fun decode(encoded: String): List<String> {
        val array = JSONArray(encoded)
        return List(array.length()) { index -> array.getString(index) }
    }
}
