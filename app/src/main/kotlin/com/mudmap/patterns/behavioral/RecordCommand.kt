package com.mudmap.patterns.behavioral

import com.mudmap.domain.TrailEntry
import com.mudmap.storage.EntryRepository

/**
 * COMMAND (GoF 233).
 * Participant: ConcreteCommand. Binds a Receiver (EntryRepository) to an
 * action (insert). Stores the inserted entry so Unexecute can remove it.
 */
class RecordCommand(
    private val receiver: EntryRepository,
    private val entry: TrailEntry
) : Command {
    override fun execute() {
        receiver.insert(entry)
    }

    override fun unexecute() {
        receiver.delete(entry.id)
    }
}
