// Pattern: Port
// Absorbs: The boundary between logging and concrete transcript extraction.
package com.mudmap.ports

import com.mudmap.storage.EntitySet

interface EntityExtractor {
    fun extract(transcript: String): EntitySet
}
