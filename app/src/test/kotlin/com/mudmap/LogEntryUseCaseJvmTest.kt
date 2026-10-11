package com.mudmap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mudmap.ports.EntityExtractor
import com.mudmap.ports.Transcriber
import com.mudmap.service.LogEntryUseCase
import com.mudmap.storage.EntitySet
import com.mudmap.storage.GeoPoint
import com.mudmap.storage.MudMapDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LogEntryUseCaseJvmTest {
    private lateinit var database: MudMapDatabase
    private val events = mutableListOf<String>()
    private val transcriber = object : Transcriber {
        override suspend fun transcribe(audioPath: String): String {
            events.add("transcribe")
            return "saw a heron by the creek, trail was muddy"
        }
    }
    private val extractor = object : EntityExtractor {
        override fun extract(transcript: String): EntitySet {
            events.add("extract")
            return EntitySet(listOf("heron"), listOf("mud"))
        }
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MudMapDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun logsAndPersistsWithoutGps() = runBlocking {
        val entry = LogEntryUseCase(transcriber, extractor, database.entryDao())
            .log("/unused/audio.m4a", null)
        assertEquals(listOf("transcribe", "extract"), events)
        assertEquals(listOf("heron"), entry.species)
        assertEquals(listOf("mud"), entry.conditions)
        assertNull(entry.geoPoint)
        assertTrue(entry.id > 0)
        assertEquals(entry, database.entryDao().getById(entry.id))
    }

    @Test
    fun preservesGpsAndReadsAllEntries() = runBlocking {
        val point = GeoPoint(37.77, -122.44)
        val entry = LogEntryUseCase(transcriber, extractor, database.entryDao())
            .log("/unused/audio.m4a", point)
        assertEquals(point, database.entryDao().getAll().single().geoPoint)
        assertEquals(entry, database.entryDao().getAll().single())
    }

    @Test
    fun doesNotInsertWhenTranscriptionFails() = runBlocking {
        val broken = object : Transcriber {
            override suspend fun transcribe(audioPath: String): String =
                error("Expected transcription failure")
        }
        try {
            LogEntryUseCase(broken, extractor, database.entryDao())
                .log("/unused/audio.m4a", null)
            throw AssertionError("Expected failure")
        } catch (_: IllegalStateException) {
            assertTrue(database.entryDao().getAll().isEmpty())
            assertTrue(events.isEmpty())
        }
    }

    @Test
    fun convertersPreservePunctuationAndUnicode() {
        val converter = com.mudmap.storage.StringListConverters()
        val tags = listOf("washed out", "quote\"slash\\", "é")
        assertEquals(tags, converter.decode(converter.encode(tags)))
        assertEquals(emptyList<String>(), converter.decode(converter.encode(emptyList())))
    }
}
