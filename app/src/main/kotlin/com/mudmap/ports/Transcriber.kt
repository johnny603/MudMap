// Pattern: Port
// Absorbs: The boundary between logging and concrete audio transcription.
package com.mudmap.slice1.ports

interface Transcriber {
    suspend fun transcribe(audioPath: String): String
}
