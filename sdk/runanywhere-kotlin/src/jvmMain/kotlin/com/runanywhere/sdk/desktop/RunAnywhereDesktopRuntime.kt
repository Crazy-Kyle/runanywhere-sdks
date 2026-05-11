package com.runanywhere.sdk.desktop

import com.runanywhere.sdk.foundation.bridge.CppBridge
import com.runanywhere.sdk.native.bridge.RunAnywhereBridge
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.SDKEnvironment
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions
import com.runanywhere.sdk.public.extensions.generateStream
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * JVM Desktop facade for RunAnywhere local text generation.
 *
 * This facade keeps desktop callers away from Android-specific initialization details while the
 * native runtime packaging is completed for macOS, Windows, and Linux.
 */
class RunAnywhereDesktopRuntime(
    private val platform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.current(),
) {
    private var llmHandle: Long = 0L
    private var loadedModelKey: String? = null

    /** Returns true when the shared RunAnywhere JNI runtime is available to the JVM process. */
    fun isAvailable(runtimeDir: String? = null): Boolean {
        if (CppBridge.isNativeLibraryLoaded) {
            return true
        }
        return RunAnywhereDesktopNativeLoader(platform = platform).load(explicitRuntimeDir = runtimeDir).isLoaded
    }

    /** Stable platform name used by diagnostics and native resource directories. */
    fun platformName(): String = platform.resourceDirectoryName

    /** Describes whether the JVM Desktop native runtime can execute local inference. */
    fun describeAvailability(runtimeDir: String? = null): String = RunAnywhereDesktopNativeLoader(platform = platform)
        .load(explicitRuntimeDir = runtimeDir)
        .describe()

    /** Initializes RunAnywhere in development mode for local desktop inference. */
    fun initialize(runtimeDir: String? = null) {
        if (!isAvailable(runtimeDir = runtimeDir)) {
            error(describeAvailability(runtimeDir = runtimeDir))
        }
        RunAnywhere.initialize(environment = SDKEnvironment.DEVELOPMENT)
    }

    /** Loads or reuses a local text model for direct JVM desktop generation. */
    fun loadTextModel(modelPath: String, modelId: String) {
        require(modelPath.isNotBlank()) { "RunAnywhere model path is required for desktop local inference." }
        require(modelId.isNotBlank()) { "RunAnywhere model ID is required for desktop local inference." }
        val nextModelKey: String = "$modelId|$modelPath"
        if (loadedModelKey == nextModelKey && llmHandle != 0L && RunAnywhereBridge.racLlmComponentIsLoaded(llmHandle)) {
            return
        }
        unloadTextModel()
        llmHandle = RunAnywhereBridge.racLlmComponentCreate()
        val resultCode: Int = RunAnywhereBridge.racLlmComponentLoadModel(
            handle = llmHandle,
            modelPath = modelPath,
            modelId = modelId,
            modelName = modelId,
        )
        if (resultCode != 0) {
            val failedHandle: Long = llmHandle
            llmHandle = 0L
            RunAnywhereBridge.racLlmComponentDestroy(failedHandle)
            error("RunAnywhere failed to load local model '$modelId' from '$modelPath' with code $resultCode.")
        }
        loadedModelKey = nextModelKey
    }

    /** Streams tokens from the loaded local LLM model using the direct JNI component facade. */
    fun generateTextStream(
        prompt: String,
        options: LLMGenerationOptions? = null,
    ): Flow<String> = callbackFlow {
        check(llmHandle != 0L) { "RunAnywhere local text model must be loaded before generation." }
        val result: String? = RunAnywhereBridge.racLlmComponentGenerateStreamWithCallback(
            handle = llmHandle,
            prompt = prompt,
            optionsJson = null,
            tokenCallback = RunAnywhereBridge.TokenCallback { tokenBytes: ByteArray ->
                trySend(tokenBytes.decodeToString()).isSuccess
            },
        )
        if (result == null) {
            close(IllegalStateException("RunAnywhere local text generation failed."))
        } else {
            close()
        }
        awaitClose()
    }

    /** Streams tokens from the currently loaded local LLM model through the public SDK API. */
    fun generateStream(
        prompt: String,
        options: LLMGenerationOptions? = null,
    ): Flow<String> {
        if (!isAvailable()) {
            error(describeAvailability())
        }
        return RunAnywhere.generateStream(prompt, options)
    }

    /** Releases the direct LLM component if it was created. */
    fun unloadTextModel() {
        if (llmHandle == 0L) {
            return
        }
        RunAnywhereBridge.racLlmComponentUnload(llmHandle)
        RunAnywhereBridge.racLlmComponentDestroy(llmHandle)
        llmHandle = 0L
        loadedModelKey = null
    }
}
