package com.mudmap.storage

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trail_entries")
data class TrailEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val recordedAt: Long,
    @Embedded(prefix = "gps_")
    val geoPoint: GeoPoint?,
    val transcript: String,
    val species: List<String>,
    val conditions: List<String>
)
