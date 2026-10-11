package com.mudmap.patterns.behavioral

/**
 * MEMENTO (GoF 283).
 * Participant: Memento. Opaque to Caretaker; only Originator (TrailEntry)
 * may read or write its contents. Wide interface is `internal` to the
 * originator's module; Caretaker sees only the abstract type.
 */
interface Memento

/**
 * Participant: Originator. TrailEntry implements createMemento()/restore().
 * See `domain/TrailEntry.kt` for the originator side.
 */
