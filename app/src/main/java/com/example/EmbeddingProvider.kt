package com.example

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

interface EmbeddingProvider {
    suspend fun embed(text: String): FloatArray
}

/**
 * A highly robust, deterministic mathematical mock provider labeled as DEVELOPMENT_EMBEDDING.
 * Implements standard sine-frequency distribution hashes to establish vector comparisons.
 * Does NOT require neural network weights and serves as a fast CPU test utility.
 */
class DevelopmentEmbeddingProvider : EmbeddingProvider {
    override suspend fun embed(text: String): FloatArray {
        val size = 128
        val vector = FloatArray(size)
        if (text.isEmpty()) return vector

        // Labeled signature prefix
        val signaturePrefix = "DEVELOPMENT_EMBEDDING:"

        // Deterministic frequency assignment
        for (i in 0 until size) {
            var sum = 0.0f
            for (charIdx in text.indices) {
                val charVal = text[charIdx].code
                val weight = sin((charIdx * i + 1).toDouble()).toFloat()
                sum += charVal * weight
            }
            vector[i] = abs(sum % 10.0f) / 10.0f
        }

        // Standard dynamic range normalization (L2 norm) to guarantee cosine similarity calculations
        var norm = 0.0f
        for (v in vector) {
            norm += v * v
        }
        norm = sqrt(norm)
        if (norm > 0f) {
            for (i in 0 until size) {
                vector[i] /= norm
            }
        }
        return vector
    }
}
