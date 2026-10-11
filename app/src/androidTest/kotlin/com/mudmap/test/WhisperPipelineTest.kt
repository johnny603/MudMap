package com.mudmap.test

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mudmap.adapters.AudioDecoder
import com.mudmap.adapters.WhisperCppAdapter
import com.mudmap.extractors.RuleBasedExtractor
import com.mudmap.service.LogEntryUseCase
import com.mudmap.storage.GeoPoint
import com.mudmap.storage.MudMapDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhisperPipelineTest {
    @Test
    fun decodesAacTranscribesWithNativeWhisperAndPersists() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val recording = File(context.cacheDir, "jfk-test.m4a")
        instrumentation.context.assets.open("jfk.m4a").use { input ->
            recording.outputStream().use { output -> input.copyTo(output) }
        }
        val pcm = AudioDecoder().decode(recording.absolutePath)
        assertTrue(pcm.size in 160_000..192_000)
        assertTrue(pcm.all { it.isFinite() && it in -1f..1f })
        val database = Room.inMemoryDatabaseBuilder(
            context, MudMapDatabase::class.java
        ).build()
        try {
            val entry = LogEntryUseCase(
                WhisperCppAdapter(context, AudioDecoder()),
                RuleBasedExtractor(context), database.entryDao()
            ).log(recording.absolutePath, GeoPoint(37.77, -122.44))
            assertTrue(entry.transcript.lowercase().contains("fellow americans"))
            assertEquals(entry, database.entryDao().getById(entry.id))
        } finally {
            database.close()
        }
    }
}
