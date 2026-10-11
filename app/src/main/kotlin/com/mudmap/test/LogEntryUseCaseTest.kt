package com.mudmap.test

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mudmap.service.LogEntryUseCase
import com.mudmap.storage.MudMapDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogEntryUseCaseTest {
    private lateinit var database: MudMapDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MudMapDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun logTranscribesExtractsAndPersistsWithoutGps() = runBlocking {
        val transcriber = FakeTranscriber()
        val extractor = FakeExtractor()
        val dao = database.entryDao()
        val useCase = LogEntryUseCase(transcriber, extractor, dao)

        val entry = useCase.log("/unused/recording.m4a", null)

        assertEquals(listOf("heron"), entry.species)
        assertEquals(listOf("mud"), entry.conditions)
        assertEquals("saw a heron by the creek, trail was muddy", entry.transcript)
        assertNull(entry.geoPoint)
        assertTrue(entry.id > 0L)
        assertTrue(entry.recordedAt > 0L)
        assertEquals("/unused/recording.m4a", transcriber.receivedAudioPath)
        assertEquals(entry.transcript, extractor.receivedTranscript)
        val stored = dao.getById(entry.id)
        assertNotNull(stored)
        assertEquals(entry, stored)
        assertEquals(listOf(entry), dao.getAll())
    }
}
