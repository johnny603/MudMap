package com.mudmap.patterns.behavioral

/**
 * STATE (GoF 305).
 * Participant: Context. Maintains an instance of a ConcreteState subclass
 * that defines the current state and delegates state-specific requests to it.
 */
class TrailEntryContext(
    private var state: EntryState
) {
    fun state(): EntryState = state

    fun transitionTo(next: EntryState) {
        state = next // atomic from the Context's perspective (p. 308)
    }

    fun retag() = state.onRetag(this)
    fun persist() = state.onPersist(this)
    fun sync() = state.onSync(this)
}
