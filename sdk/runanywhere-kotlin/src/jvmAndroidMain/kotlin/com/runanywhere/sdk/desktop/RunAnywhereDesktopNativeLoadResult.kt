package com.runanywhere.sdk.desktop

import java.io.File

/**
 * Result of a single desktop native runtime loading attempt.
 */
data class RunAnywhereDesktopNativeLoadResult(
    val platform: RunAnywhereDesktopPlatform,
    val candidateDirectories: List<File>,
    val loadedLibraries: List<File>,
    val missingLibraries: List<String>,
    val lastErrorMessage: String?,
    val fallbackLoadAttempted: Boolean,
) {
    /** Whether all required desktop runtime libraries were loaded successfully. */
    val isLoaded: Boolean = loadedLibraries.isNotEmpty() && missingLibraries.isEmpty() && lastErrorMessage == null

    /** Human-readable diagnostic message for UI and logs. */
    fun describe(): String {
        if (!platform.isSupported) {
            return "RunAnywhere JVM Desktop native runtime does not support ${platform.resourceDirectoryName}."
        }
        if (isLoaded) {
            return "RunAnywhere JVM Desktop native runtime loaded from ${loadedLibraries.first().parentFile.absolutePath}."
        }
        val missingText: String = missingLibraries.joinToString(", ").ifBlank { "none" }
        val candidateText: String = candidateDirectories.joinToString(", ") { file: File -> file.absolutePath }.ifBlank { "none" }
        val errorText: String = lastErrorMessage ?: "none"
        return "RunAnywhere JVM Desktop native runtime unavailable for ${platform.resourceDirectoryName}. Missing: $missingText. Candidates: $candidateText. Last error: $errorText. JVM Desktop requires dynamic JNI libraries, not static RACommons archives or Swift XCFrameworks."
    }
}
