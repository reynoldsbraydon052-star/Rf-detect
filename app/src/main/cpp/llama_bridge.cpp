#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_LlamaCppNative_loadModel(JNIEnv *env, jobject thiz, jstring model_path) {
    if (!model_path) {
        LOGE("loadModel called with null model_path");
        return 0;
    }

    const char *path_chars = env->GetStringUTFChars(model_path, nullptr);
    if (!path_chars) {
        LOGE("Failed to extract model_path string characters");
        return 0;
    }

    LOGI("Loading offline GGUF model from path: %s", path_chars);
    env->ReleaseStringUTFChars(model_path, path_chars);

    // Return a dummy context pointer (e.g., 1337) representing success for now
    return static_cast<jlong>(1337);
}

JNIEXPORT jstring JNICALL
Java_com_example_LlamaCppNative_generateTokens(JNIEnv *env, jobject thiz, jlong context_ptr, jstring prompt) {
    if (!prompt) {
        LOGE("generateTokens called with null prompt");
        return env->NewStringUTF("Error: null prompt");
    }

    const char *prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    if (!prompt_chars) {
        LOGE("Failed to extract prompt string characters");
        return env->NewStringUTF("Error: string conversion failure");
    }

    LOGI("Generating tokens for prompt: %s (Context Pointer: %lld)", prompt_chars, (long long)context_ptr);
    env->ReleaseStringUTFChars(prompt, prompt_chars);

    std::string response = "Simulated local LLM response for tactical query";
    return env->NewStringUTF(response.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_LlamaCppNative_freeModel(JNIEnv *env, jobject thiz, jlong context_ptr) {
    LOGI("Freeing model context at pointer: %lld", (long long)context_ptr);
}

}
