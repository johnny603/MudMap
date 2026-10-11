package com.mudmap.patterns.behavioral

/**
 * STATE (GoF 305).
 * Participant: State. Defines the interface for encapsulating the behavior
 * associated with a particular state of TrailEntry.
 * ConcreteState subclasses appear in `domain/states/`.
 */
interface EntryState {
    fun name(): String
    fun onRetag(ctx: TrailEntryContext)
    fun onPersist(ctx: TrailEntryContext)
    fun onSync(ctx: TrailEntryContext)
}
