package com.runanywhere.sdk

import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.SDKEnvironment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SDKTest {
    @Test
    fun testSDKInitialization() =
        runBlocking {
            RunAnywhere.initialize(
                apiKey = "test-api-key",
                environment = SDKEnvironment.DEVELOPMENT,
            )
            val isInitialized: Boolean = RunAnywhere.isInitialized
            println("SDK initialized: $isInitialized")
            RunAnywhere.cleanup()
        }
}
