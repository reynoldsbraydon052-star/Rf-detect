package com.example

enum class AnomalyCategory {
    NORMAL,
    LOW_DEVIATION,
    MODERATE_DEVIATION,
    HIGH_DEVIATION
}

data class AnomalyExplanation(
    val description: String,
    val scoreImpact: Int
)

data class AnomalyResult(
    val score: Int = 0, // 0 to 100
    val confidence: Float = 0f, // 0.0 to 1.0
    val category: AnomalyCategory = AnomalyCategory.NORMAL,
    val explanations: List<AnomalyExplanation> = emptyList(),
    val previousScore: Int? = null
)
