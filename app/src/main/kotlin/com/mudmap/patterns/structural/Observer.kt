package com.mudmap.patterns.structural

import com.mudmap.domain.TrailEntry

/**
 * OBSERVER (GoF 293).
 * Participant: Observer. Defines an updating interface for objects that
 * should be notified of changes in a Subject.
 * Push model is used (p. 298): the Subject sends the changed entry.
 */
interface EntryObserver {
    fun onEntriesChanged(entries: List<TrailEntry>)
}

/**
 * Participant: Subject. Knows its Observers, provides attach/detach,
 * and broadcasts a single notification to all interested Observers.
 */
interface EntrySubject {
    fun attach(o: EntryObserver)
    fun detach(o: EntryObserver)
    fun notifyObservers(entries: List<TrailEntry>)
}
