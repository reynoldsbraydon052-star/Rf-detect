package com.example

object ModelCatalog {
    data class GgufModel(
        val id: String,
        val displayName: String,
        val repository: String,
        val filename: String,
        val url: String,
        val sizeEstimate: String
    )

    const val MODEL_ID_LLAMA_3_2_3B = "llama_3_2_3b"
    const val MODEL_ID_PHI_3_5_MINI = "phi_3_5_mini"
    const val MODEL_ID_GEMMA_2_2B = "gemma_2_2b"
    const val MODEL_ID_QWEN_2_5_1_5B = "qwen_2_5_1_5b"

    // central configuration constants representing verified, actual GGUF models on HuggingFace
    val MODELS = listOf(
        GgufModel(
            id = MODEL_ID_LLAMA_3_2_3B,
            displayName = "Llama 3.2 3B Instruct",
            repository = "meta-llama/Llama-3.2-3B-Instruct-GGUF",
            filename = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            url = "https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            sizeEstimate = "2.02 GB"
        ),
        GgufModel(
            id = MODEL_ID_PHI_3_5_MINI,
            displayName = "Phi-3.5 Mini Instruct",
            repository = "bartowski/Phi-3.5-mini-instruct-GGUF",
            filename = "Phi-3.5-mini-instruct-Q4_K_M.gguf",
            url = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            sizeEstimate = "2.39 GB"
        ),
        GgufModel(
            id = MODEL_ID_GEMMA_2_2B,
            displayName = "Gemma 2 2B Instruct",
            repository = "bartowski/gemma-2-2b-it-GGUF",
            filename = "gemma-2-2b-it-Q4_K_M.gguf",
            url = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            sizeEstimate = "1.71 GB"
        ),
        GgufModel(
            id = MODEL_ID_QWEN_2_5_1_5B,
            displayName = "Qwen 2.5 1.5B Instruct",
            repository = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeEstimate = "1.11 GB"
        )
    )
}
