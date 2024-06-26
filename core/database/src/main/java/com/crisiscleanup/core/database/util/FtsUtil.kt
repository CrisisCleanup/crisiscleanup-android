package com.crisiscleanup.core.database.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log

/**
 * Formats a single token for an FTS search query.
 *
 * 1. Removes single quotes
 * 2. Escapes double quotes
 */
val String.ftsSanitize: String
    get() = replace(Regex.fromLiteral("'"), "")
        .replace(Regex.fromLiteral("\""), "\"\"")

val String.ftsGlobEnds: String
    get() = "*$this*"

val String.ftsSanitizeAsToken: String
    get() = "\"$ftsSanitize\""

// https://medium.com/android-news/offline-full-text-search-in-android-ios-b4dd5bed3acd
// https://github.com/pallocchi/movies/blob/fts4/android/app/src/main/java/com/github/movies/OkapiBM25.kt

/**
 * Convert byte array to int array (little endian).
 */
val ByteArray.intArray: Array<Int>
    get() {
        val intBuf = ByteBuffer.wrap(this)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asIntBuffer()
        val array = IntArray(intBuf.remaining())
        intBuf.get(array)
        return array.toTypedArray()
    }

internal fun Array<Int>.okapiBm25Score(
    column: Int,
    b: Double = 0.75,
    k1: Double = 1.2,
): Double {
    val pOffset = 0
    val cOffset = 1
    val nOffset = 2
    val aOffset = 3

    val termCount = this[pOffset]
    val colCount = this[cOffset]

    val lOffset = aOffset + colCount
    val xOffset = lOffset + colCount

    val totalDocs = this[nOffset].toDouble()
    val avgLength = this[aOffset + column].toDouble()
    val docLength = this[lOffset + column].toDouble()

    var score = 0.0

    for (i in 0 until termCount) {
        val currentX = xOffset + (3 * (column + i * colCount))
        val termFrequency = this[currentX].toDouble()
        val docsWithTerm = this[currentX + 2].toDouble()

        val p = totalDocs - docsWithTerm + 0.5
        val q = docsWithTerm + 0.5
        val idf = log(p, q)

        val r = termFrequency * (k1 + 1)
        val s = b * (docLength / avgLength)
        val t = termFrequency + (k1 * (1 - b + s))
        val rightSide = r / t

        score += (idf * rightSide)
    }

    return score
}
