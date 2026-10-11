package com.mudmap.patterns.behavioral.states

import com.mudmap.patterns.behavioral.EntryState
import com.mudmap.patterns.behavioral.TrailEntryContext

/** STATE (GoF 305). Participant: ConcreteState. */
class Tagged : EntryState {
    override fun name() = "Tagged"
    override fun onRetag(ctx: TrailEntryContext) = Unit
    override fun onPersist(ctx: TrailEntryContext) = Unit
    override fun onSync(ctx: TrailEntryContext) = Unit
}
