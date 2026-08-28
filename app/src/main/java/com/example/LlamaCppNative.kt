package com.example

object LlamaCppNative {
    init {
        System.loadLibrary("llama_bridge")
    }

    external fun loadModel(modelPath: String): Long
    external fun generateTokens(contextPtr: Long, prompt: String): String
    external fun freeModel(contextPtr: Long)
}
