package com.runanywhere.sdk

import com.runanywhere.sdk.desktop.RunAnywhereDesktopRuntime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class RunAnywhereDesktopRuntimeTest {
    @Test
    fun describesMissingDesktopNativeRuntimeBeforeModelLoad() {
        val runtime = RunAnywhereDesktopRuntime()
        val actualDescription: String = runtime.describeAvailability()
        assertFalse(runtime.isAvailable())
        assertContains(actualDescription, "JVM Desktop")
        assertContains(actualDescription, "native")
    }
}
