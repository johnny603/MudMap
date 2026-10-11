package com.mudmap

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mudmap.extractors.RuleBasedExtractor
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RuleBasedExtractorTest {
    private lateinit var extractor: RuleBasedExtractor

    @Before
    fun setUp() {
        extractor = RuleBasedExtractor(
            ApplicationProvider.getApplicationContext<Context>()
        )
    }

    @Test
    fun ignoresCaseAndPunctuation() {
        val entities = extractor.extract("HERON! Mud; creek. Blackberries?")
        assertEquals(listOf("heron", "blackberries"), entities.species)
        assertEquals(listOf("mud", "creek"), entities.conditions)
    }

    @Test
    fun matchesWholePhrasesAcrossNormalizedPunctuation() {
        assertEquals(
            listOf("washed out"),
            extractor.extract("Trail WASHED-OUT, then washed\nout.").conditions
        )
    }

    @Test
    fun doesNotStemOrMatchSubstrings() {
        val entities = extractor.extract("herons, muddy, crowbar, sunshine, foggy")
        assertEquals(emptyList<String>(), entities.species)
        assertEquals(emptyList<String>(), entities.conditions)
    }

    @Test
    fun deDuplicatesInVocabularyOrder() {
        val entities = extractor.extract("deer heron HERON creek creek mud")
        assertEquals(listOf("heron", "deer"), entities.species)
        assertEquals(listOf("mud", "creek"), entities.conditions)
    }

    @Test
    fun treatsUnicodeLettersAsPartOfWords() {
        assertEquals(emptyList<String>(), extractor.extract("éheron heroné").species)
    }

    @Test
    fun vocabularyHasExactlyTwentyTerms() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val terms = context.assets.open("trail_words.txt").bufferedReader()
            .use { it.readLines() }
        assertEquals(20, terms.size)
        assertEquals(10, terms.count { it.startsWith("s:") })
        assertEquals(10, terms.count { it.startsWith("c:") })
    }
}
