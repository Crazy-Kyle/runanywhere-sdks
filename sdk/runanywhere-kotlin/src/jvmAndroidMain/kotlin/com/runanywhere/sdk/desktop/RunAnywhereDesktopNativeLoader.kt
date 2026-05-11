package com.runanywhere.sdk.desktop

import java.io.File

/**
 * Finds and loads RunAnywhere desktop native libraries from explicit, bundled, or development directories.
 */
class RunAnywhereDesktopNativeLoader(
    private val platform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.current(),
    private val libraryLoader: (File) -> Unit = { libraryFile: File -> System.load(libraryFile.absolutePath) },
    private val fallbackLibraryLoader: (String) -> Unit = { libraryName: String -> System.loadLibrary(libraryName) },
) {
    /** Attempts to load the commons JNI runtime and returns structured diagnostics. */
    fun load(explicitRuntimeDir: String? = System.getProperty(NATIVE_DIR_PROPERTY)): RunAnywhereDesktopNativeLoadResult {
        if (!platform.isSupported) {
            return RunAnywhereDesktopNativeLoadResult(
                platform = platform,
                candidateDirectories = emptyList(),
                loadedLibraries = emptyList(),
                missingLibraries = emptyList(),
                lastErrorMessage = null,
                fallbackLoadAttempted = false,
            )
        }
        val candidateDirectories: List<File> = buildCandidateDirectories(explicitRuntimeDir)
        val directoryResult: RunAnywhereDesktopNativeLoadResult? = loadFromCandidateDirectories(candidateDirectories)
        if (directoryResult?.isLoaded == true) {
            return directoryResult
        }
        return loadWithFallback(candidateDirectories, directoryResult)
    }

    private fun buildCandidateDirectories(explicitRuntimeDir: String?): List<File> = listOfNotNull(
        explicitRuntimeDir?.takeIf { value: String -> value.isNotBlank() }?.let { value: String -> File(value) },
        System.getProperty(BUNDLED_NATIVE_DIR_PROPERTY)?.takeIf { value: String -> value.isNotBlank() }?.let { value: String -> File(value, platform.resourceDirectoryName) },
        File("src/jvmMain/resources/$RESOURCE_ROOT/${platform.resourceDirectoryName}").takeIf { file: File -> file.exists() },
    ).distinctBy { file: File -> file.absolutePath }

    private fun loadFromCandidateDirectories(candidateDirectories: List<File>): RunAnywhereDesktopNativeLoadResult? {
        candidateDirectories.forEach { candidateDirectory: File ->
            val missingLibraries: List<String> = platform.requiredLibraries
                .map { library: RunAnywhereNativeLibrary -> library.fileName(platform) }
                .filterNot { fileName: String -> File(candidateDirectory, fileName).isFile }
            if (missingLibraries.isEmpty()) {
                return loadFromDirectory(candidateDirectory, candidateDirectories)
            }
        }
        val allMissingLibraries: List<String> = platform.requiredLibraries
            .map { library: RunAnywhereNativeLibrary -> library.fileName(platform) }
            .filter { fileName: String -> candidateDirectories.none { directory: File -> File(directory, fileName).isFile } }
        if (candidateDirectories.isEmpty() && allMissingLibraries.isEmpty()) {
            return null
        }
        return RunAnywhereDesktopNativeLoadResult(
            platform = platform,
            candidateDirectories = candidateDirectories,
            loadedLibraries = emptyList(),
            missingLibraries = allMissingLibraries,
            lastErrorMessage = null,
            fallbackLoadAttempted = false,
        )
    }

    private fun loadFromDirectory(candidateDirectory: File, candidateDirectories: List<File>): RunAnywhereDesktopNativeLoadResult {
        val loadedLibraries: MutableList<File> = mutableListOf()
        return try {
            platform.requiredLibraries.forEach { library: RunAnywhereNativeLibrary ->
                val libraryFile: File = File(candidateDirectory, library.fileName(platform))
                libraryLoader(libraryFile)
                loadedLibraries += libraryFile
            }
            RunAnywhereDesktopNativeLoadResult(
                platform = platform,
                candidateDirectories = candidateDirectories,
                loadedLibraries = loadedLibraries,
                missingLibraries = emptyList(),
                lastErrorMessage = null,
                fallbackLoadAttempted = false,
            )
        } catch (error: UnsatisfiedLinkError) {
            RunAnywhereDesktopNativeLoadResult(
                platform = platform,
                candidateDirectories = candidateDirectories,
                loadedLibraries = loadedLibraries,
                missingLibraries = emptyList(),
                lastErrorMessage = error.message,
                fallbackLoadAttempted = false,
            )
        }
    }

    private fun loadWithFallback(
        candidateDirectories: List<File>,
        directoryResult: RunAnywhereDesktopNativeLoadResult?,
    ): RunAnywhereDesktopNativeLoadResult {
        return try {
            fallbackLibraryLoader(FALLBACK_LIBRARY_NAME)
            RunAnywhereDesktopNativeLoadResult(
                platform = platform,
                candidateDirectories = candidateDirectories,
                loadedLibraries = listOf(File(FALLBACK_LIBRARY_NAME)),
                missingLibraries = emptyList(),
                lastErrorMessage = null,
                fallbackLoadAttempted = true,
            )
        } catch (error: UnsatisfiedLinkError) {
            RunAnywhereDesktopNativeLoadResult(
                platform = platform,
                candidateDirectories = candidateDirectories,
                loadedLibraries = emptyList(),
                missingLibraries = directoryResult?.missingLibraries.orEmpty(),
                lastErrorMessage = error.message,
                fallbackLoadAttempted = true,
            )
        }
    }

    companion object {
        const val NATIVE_DIR_PROPERTY: String = "runanywhere.native.dir"
        const val BUNDLED_NATIVE_DIR_PROPERTY: String = "runanywhere.bundled.native.dir"
        const val RESOURCE_ROOT: String = "runanywhere-native"
        private const val FALLBACK_LIBRARY_NAME: String = "runanywhere_jni"
    }
}
