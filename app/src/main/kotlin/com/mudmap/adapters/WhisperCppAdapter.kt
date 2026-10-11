// Pattern: Adapter
// Absorbs: whisper.cpp library loading, model staging, PCM input, and JNI calls.
package com.mudmap.adapters

import android.content.Context
import com.mudmap.ports.Transcriber
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

    // The instance JNI shim reads this field.
    @JvmField
    val modelPath: String = File(
        appContext.filesDir,
        "ggml-base.en.bin"
    ).absolutePath

    override suspend fun transcribe(audioPath: String): String =
        withContext(Dispatchers.IO) {
            try {
                System.loadLibrary("mudmap_whisper")
            } catch (error: UnsatisfiedLinkError) {
                throw IllegalStateException("The whisper native library is unavailable.", error)
            }
            val model = File(modelPath)
            val expectedBytes = try {
                appContext.assets.openFd("ggml-base.en.bin").use { it.length }
            } catch (error: Exception) {
                throw IllegalStateException("Run scripts/prepare-assets.sh before building.", error)
            }
            require(expectedBytes > 0L) { "The bundled whisper model is empty." }
            if (model.length() != expectedBytes) {
                val staging = File("$modelPath.part")
                appContext.assets.open("ggml-base.en.bin").use { input ->
                    staging.outputStream().use { output -> input.copyTo(output) }
                }
                require(staging.length() == expectedBytes && staging.renameTo(model)) {
                    "Could not stage the whisper model in private storage."
                }
            }
            currentCoroutineContext().ensureActive()
            val samples = decoder.decode(audioPath)
            require(samples.isNotEmpty()) { "The recording contains no PCM." }
            currentCoroutineContext().ensureActive()
            whisperTranscribe(samples).trim()
        }

    // Implemented by app/src/main/cpp/whisper_jni.cpp.
    external fun whisperTranscribe(samples: FloatArray): String
}
