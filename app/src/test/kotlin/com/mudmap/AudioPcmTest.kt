package com.mudmap

import android.media.AudioFormat
import com.mudmap.adapters.AudioDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPcmTest {
    private val decoder = AudioDecoder()

    @Test
    fun downmixesSigned16BitStereo() {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        bytes.putShort(32767).putShort(-32768).putShort(16384).putShort(16384)
        bytes.flip()
        val samples = AudioDecoder.FloatBuffer()
        decoder.appendMono(bytes, 2, AudioFormat.ENCODING_PCM_16BIT, samples)
        assertArrayEquals(floatArrayOf(-1f / 65536, 0.5f), samples.toArray(), 0.00001f)
    }

    @Test
    fun decodesUnsigned8BitPcm() {
        val samples = AudioDecoder.FloatBuffer()
        decoder.appendMono(
            ByteBuffer.wrap(byteArrayOf(0, 128.toByte(), 255.toByte())),
            1, AudioFormat.ENCODING_PCM_8BIT, samples
        )
        assertArrayEquals(floatArrayOf(-1f, 0f, 127f / 128), samples.toArray(), 0f)
    }

    @Test
    fun clampsFloatPcmAndReplacesNonFiniteSamples() {
        val bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        listOf(-2f, 2f, Float.NaN, Float.POSITIVE_INFINITY).forEach(bytes::putFloat)
        bytes.flip()
        val samples = AudioDecoder.FloatBuffer()
        decoder.appendMono(bytes, 1, AudioFormat.ENCODING_PCM_FLOAT, samples)
        assertArrayEquals(floatArrayOf(-1f, 1f, 0f, 0f), samples.toArray(), 0f)
    }

    @Test
    fun convertsOneSecondAt48KhzTo16Khz() {
        val input = FloatArray(48_000) { it.toFloat() / 48_000 }
        val output = decoder.resample(input, 48_000)
        assertEquals(16_000, output.size)
        assertEquals(input[3], output[1], 0f)
        assertTrue(output.all { it in -1f..1f })
    }

    @Test
    fun linearlyInterpolatesUpsampling() {
        assertArrayEquals(
            floatArrayOf(0f, 0.5f, 1f, 1f),
            decoder.resample(floatArrayOf(0f, 1f), 8_000), 0f
        )
    }

    @Test
    fun acceptsEmptyPcm() {
        assertEquals(0, decoder.resample(floatArrayOf(), 44_100).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidSampleRate() {
        decoder.resample(floatArrayOf(0f), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncompleteFrames() {
        decoder.appendMono(
            ByteBuffer.wrap(byteArrayOf(1)), 1, AudioFormat.ENCODING_PCM_16BIT,
            AudioDecoder.FloatBuffer()
        )
    }
}
