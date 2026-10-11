// Pattern: Adapter
// Absorbs: The trail vocabulary asset format and exact phrase-matching rules.
package com.mudmap.extractors

import android.content.Context
import com.mudmap.ports.EntityExtractor
import com.mudmap.storage.EntitySet
import java.util.Locale

class RuleBasedExtractor(context: Context) : EntityExtractor {
    private val speciesWords = linkedSetOf<String>()
    private val conditionWords = linkedSetOf<String>()

    init {
        context.applicationContext.assets.open("trail_words.txt")
            .bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, raw ->
                    val line = raw.trim()
                    if (line.isNotEmpty()) {
                        require(line.length > 2 && line[1] == ':') {
                            "Invalid vocabulary line ${index + 1}."
                        }
                        val term = normalize(line.substring(2))
                        require(term.isNotEmpty()) {
                            "Empty vocabulary term on line ${index + 1}."
                        }
                        when (line[0].lowercaseChar()) {
                            's' -> speciesWords.add(term)
                            'c' -> conditionWords.add(term)
                            else -> error("Unknown vocabulary prefix on line ${index + 1}.")
                        }
                    }
                }
            }
    }

    override fun extract(transcript: String): EntitySet {
        val text = normalize(transcript)
        fun matches(term: String): Boolean {
            val phrase = term.split(' ').joinToString("\\s+") { Regex.escape(it) }
            return Regex(
                "(?<![\\p{L}\\p{N}])$phrase(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(text)
        }
        return EntitySet(
            species = speciesWords.filter(::matches),
            conditions = conditionWords.filter(::matches)
        )
    }

    private fun normalize(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
