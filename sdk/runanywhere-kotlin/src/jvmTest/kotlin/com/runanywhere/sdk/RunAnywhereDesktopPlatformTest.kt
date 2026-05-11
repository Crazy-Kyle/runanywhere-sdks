package com.runanywhere.sdk.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunAnywhereDesktopPlatformTest {
    @Test
    fun detectsMacosAppleSiliconPlatform() {
        val actualPlatform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.detect(
            osName = "Mac OS X",
            osArch = "aarch64",
        )
        assertEquals("macos-aarch64", actualPlatform.resourceDirectoryName)
        assertEquals("dylib", actualPlatform.libraryExtension)
    }

    @Test
    fun detectsWindowsX64Platform() {
        val actualPlatform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.detect(
            osName = "Windows 11",
            osArch = "amd64",
        )
        assertEquals("windows-x64", actualPlatform.resourceDirectoryName)
        assertEquals("dll", actualPlatform.libraryExtension)
    }

    @Test
    fun detectsLinuxX64Platform() {
        val actualPlatform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.detect(
            osName = "Linux",
            osArch = "x86_64",
        )
        assertEquals("linux-x64", actualPlatform.resourceDirectoryName)
        assertEquals("so", actualPlatform.libraryExtension)
    }

    @Test
    fun reportsUnsupportedPlatform() {
        val actualPlatform: RunAnywhereDesktopPlatform = RunAnywhereDesktopPlatform.detect(
            osName = "Solaris",
            osArch = "sparc",
        )
        assertFalse(actualPlatform.isSupported)
        assertEquals("unsupported-sparc", actualPlatform.resourceDirectoryName)
    }
}
