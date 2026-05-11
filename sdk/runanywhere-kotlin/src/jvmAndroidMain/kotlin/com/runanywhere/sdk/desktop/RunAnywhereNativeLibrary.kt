package com.runanywhere.sdk.desktop

/**
 * Native library participating in the RunAnywhere desktop JNI runtime.
 */
enum class RunAnywhereNativeLibrary(
    private val unixBaseName: String,
    private val windowsBaseName: String,
) {
    RacCommons("rac_commons", "rac_commons"),
    RunAnywhereJni("runanywhere_jni", "runanywhere_jni"),
    LlamaCppBackend("rac_backend_llamacpp", "rac_backend_llamacpp"),
    LlamaCppJni("rac_backend_llamacpp_jni", "rac_backend_llamacpp_jni"),
    OnnxBackend("rac_backend_onnx", "rac_backend_onnx"),
    OnnxJni("rac_backend_onnx_jni", "rac_backend_onnx_jni"),
    ;

    /** Returns the concrete native file name for the target desktop platform. */
    fun fileName(platform: RunAnywhereDesktopPlatform): String {
        if (platform.osName == "windows") {
            return "$windowsBaseName.${platform.libraryExtension}"
        }
        return "lib$unixBaseName.${platform.libraryExtension}"
    }
}
