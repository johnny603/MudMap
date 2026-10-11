package com.mudmap.patterns.behavioral

import com.mudmap.domain.TrailEntry
import com.mudmap.storage.EntryRepository

/**
 * COMMAND (GoF 233).
 * Participant: ConcreteCommand. Uses a MEMENTO (GoF 283) to restore the
 * deleted entry on Unexecute, per the book's advice on p. 239 to avoid
 * error accumulation during undo/redo.
 */
class DeleteCommand(
    private val receiver: EntryRepository,
    private val entry: TrailEntry
) : Command {
    private var snapshot: Memento? = null

    override fun execute() {
        snapshot = entry.createMemento()
        receiver.delete(entry.id)
    }

    override fun unexecute() {
        val m = snapshot ?: return
        entry.restore(m)
        receiver.insert(entry)
    }
}
