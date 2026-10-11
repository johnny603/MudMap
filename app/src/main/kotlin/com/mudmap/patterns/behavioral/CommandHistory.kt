package com.mudmap.patterns.behavioral

/**
 * COMMAND (GoF 233), Command History (p. 62).
 * Participant: Invoker. Stores executed Commands on a history list and
 * supports arbitrary-level undo/redo by walking the list.
 * Copying-on-execute is required per p. 238 because command state can vary
 * across invocations (e.g. DeleteEntry with different targets).
 */
class CommandHistory {
    private val past = ArrayDeque<Command>()
    private val future = ArrayDeque<Command>()

    fun execute(cmd: Command) {
        cmd.execute()
        if (cmd.reversible()) {
            past.addLast(cmd)
            future.clear() // new branch invalidates redo
        }
    }

    fun undo() {
        val cmd = past.removeLastOrNull() ?: return
        cmd.unexecute()
        future.addLast(cmd)
    }

    fun redo() {
        val cmd = future.removeLastOrNull() ?: return
        cmd.execute()
        past.addLast(cmd)
    }
}
