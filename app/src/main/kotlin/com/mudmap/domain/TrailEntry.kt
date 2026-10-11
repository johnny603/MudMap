package com.mudmap.domain

import com.mudmap.patterns.behavioral.Memento
import com.mudmap.patterns.behavioral.TrailEntryContext

/**
 * MEMENTO (GoF 283). Participant: Originator.
 *   createMemento() returns a snapshot; restore() re-applies it.
 * STATE (GoF 305). Participant: Context (via TrailEntryContext).
 *   The domain object holds a TrailEntryContext so lifecycle is explicit.
 */
class TrailEntry(
    val id: String,
    var transcript: String,
    var tags: List<String>,
    var lat: Double?,
    var lon: Double?,
    var recordedAt: Long,
    private val ctx: TrailEntryContext
) {
    fun createMemento(): Memento = TrailEntryMemento(
        transcript, tags.toList(), lat, lon, recordedAt
    )

    fun restore(m: Memento) {
        val s = m as TrailEntryMemento
        transcript = s.transcript
        tags = s.tags
        lat = s.lat
        lon = s.lon
        recordedAt = s.recordedAt
    }

    fun lifecycle() = ctx.state().name()
}

/** MEMENTO (GoF 283). Participant: Memento. Opaque to Caretaker. */
internal class TrailEntryMemento(
    val transcript: String,
    val tags: List<String>,
    val lat: Double?,
    val lon: Double?,
    val recordedAt: Long
) : Memento
