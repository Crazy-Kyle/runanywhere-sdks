package com.runanywhere.sdk.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/** Builds and stages the desktop commons JNI runtime for the current host platform. */
abstract class BuildDesktopNativeRuntimeTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputDirectory
    abstract val commonsDir: DirectoryProperty

    @get:Input
    abstract val platformName: Property<String>

    @get:Input
    abstract val cmakeExecutable: Property<String>

    @get:Input
    abstract val requiredLibraries: ListProperty<String>

    @get:Input
    abstract val jniLibraries: ListProperty<String>

    @get:LocalState
    abstract val nativeBuildDir: DirectoryProperty

    @get:OutputDirectory
    abstract val nativeOutputDir: DirectoryProperty

    @get:OutputFiles
    val outputLibraries = project.objects.fileCollection().from(
        requiredLibraries.map { libraryNames: List<String> ->
            libraryNames.map { libraryName: String -> nativeOutputDir.get().asFile.resolve(libraryName) }
        },
    )

    @TaskAction
    fun buildRuntime() {
        val buildDirectory: File = nativeBuildDir.get().asFile
        val outputDirectory: File = nativeOutputDir.get().asFile
        outputDirectory.mkdirs()
        execOperations.exec {
            commandLine(
                cmakeExecutable.get(),
                "-S", commonsDir.get().asFile.absolutePath,
                "-B", buildDirectory.absolutePath,
                "-DCMAKE_BUILD_TYPE=Release",
                "-DRAC_BUILD_SHARED=ON",
                "-DRAC_BUILD_JNI=ON",
                "-DRAC_BUILD_BACKENDS=OFF",
                "-DRAC_BUILD_PLATFORM=OFF",
                "-DRAC_BUILD_TESTS=OFF",
            )
        }
        execOperations.exec {
            commandLine(
                cmakeExecutable.get(),
                "--build", buildDirectory.absolutePath,
                "--config", "Release",
                "--target", "rac_commons", "runanywhere_commons_jni",
            )
        }
        copyBuiltLibraries(buildDirectory, outputDirectory)
        val missingLibraries: List<String> = requiredLibraries.get().filterNot { libraryName: String ->
            outputDirectory.resolve(libraryName).isFile
        }
        if (missingLibraries.isNotEmpty()) {
            throw GradleException(
                "Desktop RunAnywhere native runtime build did not produce ${missingLibraries.joinToString(", ")} " +
                    "for ${platformName.get()} in ${outputDirectory.absolutePath}",
            )
        }
        logger.lifecycle("Desktop RunAnywhere native runtime staged at ${outputDirectory.absolutePath}")
    }

    private fun copyBuiltLibraries(buildDirectory: File, outputDirectory: File) {
        requiredLibraries.get().forEach { libraryName: String ->
            val sourceFile: File = resolveBuiltLibrary(buildDirectory, libraryName)
            sourceFile.copyTo(outputDirectory.resolve(libraryName), overwrite = true)
        }
    }

    private fun resolveBuiltLibrary(buildDirectory: File, libraryName: String): File {
        val candidates: List<File> = listOf(
            buildDirectory.resolve(libraryName),
            buildDirectory.resolve("src/jni").resolve(libraryName),
        )
        return candidates.firstOrNull { candidate: File -> candidate.isFile }
            ?: throw GradleException("CMake build did not produce $libraryName in ${candidates.joinToString { it.parentFile.absolutePath }}")
    }
}

/** Prints the desktop native runtime resource status for all supported targets. */
abstract class PrintDesktopNativeRuntimeStatusTask : DefaultTask() {
    @get:InputDirectory
    abstract val nativeResourceDir: DirectoryProperty

    @get:Input
    abstract val platformNames: ListProperty<String>

    @get:Input
    abstract val macOsLibraries: ListProperty<String>

    @get:Input
    abstract val windowsLibraries: ListProperty<String>

    @get:Input
    abstract val linuxLibraries: ListProperty<String>

    @TaskAction
    fun printStatus() {
        val resourceDir: File = nativeResourceDir.get().asFile
        logger.lifecycle("RunAnywhere desktop native resource dir: ${resourceDir.absolutePath}")
        platformNames.get().forEach { platformName: String ->
            val platformDir: File = resourceDir.resolve(platformName)
            val missingLibraries: List<String> = getRequiredLibraries(platformName).filterNot { libraryName: String -> platformDir.resolve(libraryName).isFile }
            if (missingLibraries.isEmpty()) {
                logger.lifecycle("✓ $platformName: all required libraries present")
            } else {
                logger.lifecycle("✗ $platformName: missing ${missingLibraries.joinToString(", ")}")
            }
        }
    }

    private fun getRequiredLibraries(platformName: String): List<String> = when {
        platformName.startsWith("macos-") -> macOsLibraries.get()
        platformName.startsWith("windows-") -> windowsLibraries.get()
        platformName.startsWith("linux-") -> linuxLibraries.get()
        else -> emptyList()
    }
}

/** Verifies desktop native runtime resources for the current target or all supported targets. */
abstract class VerifyDesktopNativeRuntimeTask : DefaultTask() {
    @get:InputDirectory
    abstract val nativeResourceDir: DirectoryProperty

    @get:Input
    abstract val platformNames: ListProperty<String>

    @get:Input
    abstract val macOsLibraries: ListProperty<String>

    @get:Input
    abstract val windowsLibraries: ListProperty<String>

    @get:Input
    abstract val linuxLibraries: ListProperty<String>

    @get:Input
    abstract val platformName: Property<String>

    @get:Input
    abstract val verifyAllTargets: Property<Boolean>

    @TaskAction
    fun verifyRuntime() {
        val resourceDir: File = nativeResourceDir.get().asFile
        val platformsToVerify: List<String> = if (verifyAllTargets.get()) {
            platformNames.get()
        } else {
            listOf(platformName.get())
        }
        val missingDescriptions: List<String> = platformsToVerify.flatMap { platformName: String ->
            val platformDir: File = resourceDir.resolve(platformName)
            getRequiredLibraries(platformName)
                .filterNot { libraryName: String -> platformDir.resolve(libraryName).isFile }
                .map { libraryName: String -> "$platformName/$libraryName" }
        }
        if (missingDescriptions.isNotEmpty()) {
            throw GradleException("Missing RunAnywhere desktop native runtime artifacts: ${missingDescriptions.joinToString(", ")}")
        }
        logger.lifecycle("RunAnywhere desktop native runtime verification passed for ${platformsToVerify.joinToString(", ")}")
    }

    private fun getRequiredLibraries(platformName: String): List<String> = when {
        platformName.startsWith("macos-") -> macOsLibraries.get()
        platformName.startsWith("windows-") -> windowsLibraries.get()
        platformName.startsWith("linux-") -> linuxLibraries.get()
        else -> emptyList()
    }
}
