package com.mudmap.patterns.behavioral

/**
 * COMMAND (GoF 233).
 * Participant: Command. Declares the interface for executing an operation.
 * Unexecute and Reversible are added per the book's undo/redo section (p. 62).
 */
interface Command {
    fun execute()
    fun unexecute()
    fun reversible(): Boolean = true
}
