package com.mudmap.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: TrailEntry): Long

    @Query("SELECT * FROM trail_entries ORDER BY recordedAt DESC, id DESC")
    suspend fun getAll(): List<TrailEntry>

    @Query("SELECT * FROM trail_entries WHERE id = :id")
    suspend fun getById(id: Long): TrailEntry?
}
