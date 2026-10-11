package com.mudmap.test

import com.mudmap.ports.EntityExtractor
import com.mudmap.storage.EntitySet

class FakeExtractor : EntityExtractor {
    var receivedTranscript: String? = null
        private set

    override fun extract(transcript: String): EntitySet {
        receivedTranscript = transcript
        return EntitySet(listOf("heron"), listOf("mud"))
    }
}
