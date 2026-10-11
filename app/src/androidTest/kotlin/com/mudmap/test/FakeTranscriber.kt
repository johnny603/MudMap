package com.mudmap.test

import com.mudmap.ports.Transcriber

class FakeTranscriber : Transcriber {
    var receivedAudioPath: String? = null
        private set

    override suspend fun transcribe(audioPath: String): String {
        receivedAudioPath = audioPath
        return "saw a heron by the creek, trail was muddy"
    }
}
