package com.mudmap

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.util.UUID

class RecorderController(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "recordings")
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    @Suppress("DEPRECATION")
    fun start() {
        check(recorder == null) { "A recording is already active." }
        check(directory.isDirectory || directory.mkdirs()) {
            "Could not create the private recordings directory."
        }
        val file = File(directory, "${UUID.randomUUID()}.m4a")
        val next = MediaRecorder()
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioChannels(1)
            next.setAudioSamplingRate(44_100)
            next.setAudioEncodingBitRate(96_000)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
            audioFile = file
            recorder = next
        } catch (error: Exception) {
            next.release()
            throw error
        }
    }

    fun stop(): String {
        val active = checkNotNull(recorder) { "No active recording." }
        val file = checkNotNull(audioFile)
        try {
            // Very short recordings may make MediaRecorder.stop() throw.
            active.stop()
            return file.absolutePath
        } finally {
            recorder = null
            audioFile = null
            active.release()
        }
    }

    fun release() {
        val active = recorder
        recorder = null
        audioFile = null
        active?.release()
    }
}
