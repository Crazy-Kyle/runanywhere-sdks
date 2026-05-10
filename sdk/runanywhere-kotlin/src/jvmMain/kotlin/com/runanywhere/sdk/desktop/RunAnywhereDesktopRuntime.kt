package com.runanywhere.sdk.desktop

import com.runanywhere.sdk.foundation.bridge.CppBridge
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.SDKEnvironment
import com.runanywhere.sdk.public.extensions.LLM.LLMGenerationOptions
import com.runanywhere.sdk.public.extensions.generateStream
import kotlinx.coroutines.flow.Flow

/**
 * JVM Desktop facade for RunAnywhere local text generation.
 *
 * This facade keeps desktop callers away from Android-specific initialization details while the
 * native runtime packaging is completed for macOS, Windows, and Linux.
 */
class RunAnywhereDesktopRuntime {
    /** Returns true when the shared RunAnywhere JNI runtime is available to the JVM process. */
    fun isAvailable(): Boolean = CppBridge.isNativeLibraryLoaded

    /** Describes whether the JVM Desktop native runtime can execute local inference. */
    fun describeAvailability(): String {
        if (isAvailable()) {
            return "RunAnywhere JVM Desktop native runtime is available."
        }
        return "RunAnywhere JVM Desktop native runtime is unavailable; provide desktop native libraries before local inference."
    }

    /** Initializes RunAnywhere in development mode for local desktop inference. */
    fun initialize() {
        RunAnywhere.initialize(environment = SDKEnvironment.DEVELOPMENT)
    }

    /** Streams tokens from the currently loaded local LLM model. */
    fun generateStream(
        prompt: String,
        options: LLMGenerationOptions? = null,
    ): Flow<String> {
        if (!isAvailable()) {
            error(describeAvailability())
        }
        return RunAnywhere.generateStream(prompt, options)
    }
}
