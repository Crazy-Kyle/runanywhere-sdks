package com.runanywhere.sdk.desktop

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunAnywhereDesktopNativeLoaderTest {
    @Test
    fun reportsMissingLibrariesFromExplicitDirectory() {
        val inputDirectory: File = createTempDir(prefix = "runanywhere-native-missing-")
        val platform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.MacOsArm64
        val loader = RunAnywhereDesktopNativeLoader(
            platform = platform,
            libraryLoader = { throw UnsatisfiedLinkError("should not load missing files") },
        )
        val actualResult: RunAnywhereDesktopNativeLoadResult = loader.load(explicitRuntimeDir = inputDirectory.absolutePath)
        assertFalse(actualResult.isLoaded)
        assertEquals(platform, actualResult.platform)
        assertTrue(actualResult.missingLibraries.isNotEmpty())
        assertContains(actualResult.describe(), "librunanywhere_jni.dylib")
        assertContains(actualResult.describe(), "dynamic JNI libraries")
    }

    @Test
    fun loadsLibrariesInDeclaredOrderFromExplicitDirectory() {
        val inputDirectory: File = createTempDir(prefix = "runanywhere-native-present-")
        val platform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.LinuxX64
        platform.requiredLibraries.forEach { library: RunAnywhereNativeLibrary ->
            File(inputDirectory, library.fileName(platform)).writeText("placeholder")
        }
        val actualLoadedPaths: MutableList<String> = mutableListOf()
        val loader = RunAnywhereDesktopNativeLoader(
            platform = platform,
            libraryLoader = { libraryFile: File -> actualLoadedPaths += libraryFile.name },
        )
        val actualResult: RunAnywhereDesktopNativeLoadResult = loader.load(explicitRuntimeDir = inputDirectory.absolutePath)
        assertTrue(actualResult.isLoaded)
        assertEquals(
            platform.requiredLibraries.map { library: RunAnywhereNativeLibrary -> library.fileName(platform) },
            actualLoadedPaths,
        )
    }

    @Test
    fun loadsMacOsLibrariesFromBuiltDesktopRuntimeDirectoryWhenPresent() {
        val inputDirectory = File("build/desktop-native/macos-aarch64")
        if (!inputDirectory.resolve("librac_commons.dylib").isFile || !inputDirectory.resolve("librunanywhere_jni.dylib").isFile) {
            return
        }
        val platform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.MacOsArm64
        val actualLoadedPaths: MutableList<String> = mutableListOf()
        val loader = RunAnywhereDesktopNativeLoader(
            platform = platform,
            libraryLoader = { libraryFile: File -> actualLoadedPaths += libraryFile.name },
        )
        val actualResult: RunAnywhereDesktopNativeLoadResult = loader.load(explicitRuntimeDir = inputDirectory.absolutePath)
        assertTrue(actualResult.isLoaded)
        assertEquals(
            platform.requiredLibraries.map { library: RunAnywhereNativeLibrary -> library.fileName(platform) },
            actualLoadedPaths,
        )
        assertContains(actualResult.describe(), inputDirectory.absolutePath)
    }
}
