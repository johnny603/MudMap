package com.mudmap.service

import com.mudmap.slice1.ports.EntityExtractor
import com.mudmap.slice1.ports.Transcriber
import com.mudmap.slice1.storage.EntryDao
import com.mudmap.slice1.storage.GeoPoint
import com.mudmap.slice1.storage.TrailEntry

class LogEntryUseCase(
    private val transcriber: Transcriber,
    private val extractor: EntityExtractor,
    private val dao: EntryDao
) {
    suspend fun log(audioPath: String, geoPoint: GeoPoint?): TrailEntry {
        val transcript = transcriber.transcribe(audioPath)
        val entities = extractor.extract(transcript)
        val entry = TrailEntry(
            recordedAt = System.currentTimeMillis(),
            geoPoint = geoPoint,
            transcript = transcript,
            species = entities.species,
            conditions = entities.conditions
        )
        val id = dao.insert(entry)
        return entry.copy(id = id)
    }
}
