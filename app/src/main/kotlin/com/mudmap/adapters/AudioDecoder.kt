// Decodes .m4a to 16 kHz mono float PCM for whisper.cpp. Not a pattern.
package com.mudmap.slice1.adapters

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor

class AudioDecoder {
    fun decode(audioPath: String): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var started = false
        try {
            extractor.setDataSource(audioPath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it)
                    .getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track.")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            require(mime == "audio/mp4a-latm") { "Expected AAC in .m4a." }
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val decoder = MediaCodec.createDecoderByType(mime)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            started = true
            val info = MediaCodec.BufferInfo()
            val pcm = FloatBuffer()
            var inputEnded = false
            var outputEnded = false
            var idle = 0
            while (!outputEnded) {
                var progressed = false
                if (!inputEnded) {
                    val index = decoder.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = requireNotNull(decoder.getInputBuffer(index))
                        buffer.clear()
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(
                                index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(
                                index, 0, size, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                        progressed = true
                    }
                }
                when (val index = decoder.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val output = decoder.outputFormat
                        val nextRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val nextChannels =
                            output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        require(pcm.size == 0 ||
                            (rate == nextRate && channels == nextChannels)) {
                            "Audio format changed during decoding."
                        }
                        rate = nextRate
                        channels = nextChannels
                        encoding = if (output.containsKey(MediaFormat.KEY_PCM_ENCODING))
                            output.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        else AudioFormat.ENCODING_PCM_16BIT
                        progressed = true
                    }
                    else -> if (index >= 0) {
                        try {
                            if (info.size > 0 &&
                                info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                val buffer = requireNotNull(decoder.getOutputBuffer(index))
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                appendMono(buffer.slice(), channels, encoding, pcm)
                            }
                            outputEnded =
                                info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally {
                            decoder.releaseOutputBuffer(index, false)
                        }
                        progressed = true
                    }
                }
                idle = if (progressed) 0 else idle + 1
                check(idle < 1_000) { "AAC decoder stalled." }
            }
            return resample(pcm.toArray(), rate)
        } finally {
            try {
                if (started) codec?.stop()
            } finally {
                try { codec?.release() } finally { extractor.release() }
            }
        }
    }

    private fun appendMono(
        buffer: ByteBuffer, channels: Int, encoding: Int, target: FloatBuffer
    ) {
        require(channels > 0)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val bytes = when (encoding) {
            AudioFormat.ENCODING_PCM_8BIT -> 1
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> error("Unsupported decoded PCM encoding: $encoding")
        }
        require(buffer.remaining() % (bytes * channels) == 0)
        while (buffer.hasRemaining()) {
            var sum = 0f
            repeat(channels) {
                val sample = when (encoding) {
                    AudioFormat.ENCODING_PCM_8BIT ->
                        ((buffer.get().toInt() and 255) - 128) / 128f
                    AudioFormat.ENCODING_PCM_16BIT -> buffer.short / 32768f
                    else -> buffer.float
                }
                sum += if (sample.isFinite()) sample.coerceIn(-1f, 1f) else 0f
            }
            target.add((sum / channels).coerceIn(-1f, 1f))
        }
    }

    private fun resample(input: FloatArray, rate: Int): FloatArray {
        require(rate > 0)
        if (input.isEmpty() || rate == 16_000) return input
        val count = ceil(input.size.toDouble() * 16_000 / rate).toInt()
        return FloatArray(count) { index ->
            val position = (index.toDouble() * rate / 16_000).coerceAtMost(input.lastIndex.toDouble())
            val left = floor(position).toInt()
            val right = (left + 1).coerceAtMost(input.lastIndex)
            val fraction = (position - left).toFloat()
            (input[left] + (input[right] - input[left]) * fraction).coerceIn(-1f, 1f)
        }
    }

    private class FloatBuffer {
        private var values = FloatArray(16_000)
        var size = 0
            private set
        fun add(value: Float) {
            if (size == values.size) values = values.copyOf(values.size * 2)
            values[size++] = value
        }
        fun toArray(): FloatArray = values.copyOf(size)
    }
}
