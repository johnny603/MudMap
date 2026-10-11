// Pattern: Port
// Absorbs: The boundary between logging and concrete transcript extraction.
package com.mudmap.ports

import com.mudmap.slice1.storage.EntitySet

interface EntityExtractor {
    fun extract(transcript: String): EntitySet
}
