package com.runanywhere.sdk.desktop

/**
 * Normalized JVM Desktop target used to find RunAnywhere native runtime artifacts.
 */
data class RunAnywhereDesktopPlatform(
    val osName: String,
    val architectureName: String,
    val libraryExtension: String,
    val isSupported: Boolean = true,
) {
    /** Directory name used under runanywhere-native resources and distribution folders. */
    val resourceDirectoryName: String = "$osName-$architectureName"

    /** Native libraries required by the commons JNI bridge on this platform. */
    val requiredLibraries: List<RunAnywhereNativeLibrary>
        get() = if (isSupported) COMMONS_LIBRARIES else emptyList()

    companion object {
        val MacOsArm64: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform("macos", "aarch64", "dylib")
        val MacOsX64: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform("macos", "x64", "dylib")
        val WindowsX64: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform("windows", "x64", "dll")
        val LinuxX64: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform("linux", "x64", "so")
        private val COMMONS_LIBRARIES: List<RunAnywhereNativeLibrary> = listOf(
            RunAnywhereNativeLibrary.RacCommons,
            RunAnywhereNativeLibrary.RunAnywhereJni,
        )

        /** Detects the current desktop platform from JVM system properties. */
        fun current(): RunAnywhereDesktopPlatform = detect(
            osName = System.getProperty("os.name").orEmpty(),
            osArch = System.getProperty("os.arch").orEmpty(),
        )

        /** Converts raw JVM OS and architecture names into a stable native artifact target. */
        fun detect(osName: String, osArch: String): RunAnywhereDesktopPlatform {
            val normalizedOsName: String = osName.lowercase()
            val normalizedArchitecture: String = normalizeArchitecture(osArch)
            if (normalizedOsName.contains("mac") && normalizedArchitecture == "aarch64") {
                return MacOsArm64
            }
            if (normalizedOsName.contains("mac") && normalizedArchitecture == "x64") {
                return MacOsX64
            }
            if (normalizedOsName.contains("win") && normalizedArchitecture == "x64") {
                return WindowsX64
            }
            if (normalizedOsName.contains("linux") && normalizedArchitecture == "x64") {
                return LinuxX64
            }
            return RunAnywhereDesktopPlatform("unsupported", normalizedArchitecture, "", isSupported = false)
        }

        private fun normalizeArchitecture(osArch: String): String {
            val normalizedArchitecture: String = osArch.lowercase()
            return when (normalizedArchitecture) {
                "aarch64", "arm64" -> "aarch64"
                "x86_64", "amd64", "x64" -> "x64"
                else -> normalizedArchitecture.ifBlank { "unknown" }
            }
        }
    }
}
