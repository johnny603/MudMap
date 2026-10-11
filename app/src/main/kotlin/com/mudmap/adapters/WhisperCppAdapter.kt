// Pattern: Adapter
// Absorbs: whisper.cpp library loading, model staging, PCM input, and JNI calls.
package com.mudmap.slice1.adapters

import android.content.Context
import com.mudmap.slice1.ports.Transcriber
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class WhisperCppAdapter(
    context: Context,
    private val decoder: AudioDecoder
) : Transcriber {
    private val appContext = context.applicationContext

    // The human-written JNI shim reads this instance field.
    @JvmField
    val modelPath: String = File(
        appContext.filesDir,
        "ggml-base.en.bin"
    ).absolutePath

    override suspend fun transcribe(audioPath: String): String =
        withContext(Dispatchers.IO) {
            System.loadLibrary("mudmap_whisper")
            val model = File(modelPath)
            if (!model.exists()) {
                appContext.assets.open("ggml-base.en.bin").use { input ->
                    model.outputStream().use { output -> input.copyTo(output) }
                }
            }
            require(model.length() > 0L) { "The whisper model is empty." }
            currentCoroutineContext().ensureActive()
            val samples = decoder.decode(audioPath)
            require(samples.isNotEmpty()) { "The recording contains no PCM." }
            currentCoroutineContext().ensureActive()
            whisperTranscribe(samples).trim()
        }

    // Human must implement this instance JNI method and its model loading.
    external fun whisperTranscribe(samples: FloatArray): String
}
