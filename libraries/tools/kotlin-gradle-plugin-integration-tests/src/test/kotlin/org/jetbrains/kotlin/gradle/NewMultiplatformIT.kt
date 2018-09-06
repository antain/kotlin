/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.sources.METADATA_CONFIGURATION_NAME_SUFFIX
import org.jetbrains.kotlin.gradle.plugin.sources.SourceSetConsistencyChecks
import org.jetbrains.kotlin.gradle.util.checkBytecodeContains
import org.jetbrains.kotlin.gradle.util.isWindows
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assert
import org.junit.Test
import java.util.zip.ZipFile
import kotlin.test.assertTrue

class NewMultiplatformIT : BaseGradleIT() {
    val gradleVersion = GradleVersionRequired.AtLeast("4.8")

    private fun Project.targetClassesDir(targetName: String, sourceSetName: String = "main") =
        classesDir(sourceSet = "$targetName/$sourceSetName")

    @Test
    fun testLibAndApp() {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")
        val oldStyleAppProject = Project("sample-old-style-app", gradleVersion, "new-mpp-lib-and-app")

        val hostTargetName = when {
            HostManager.hostIsMingw -> "mingw64"
            HostManager.hostIsLinux -> "linux64"
            HostManager.hostIsMac -> "macos64"
            else -> error("Unknown host")
        }

        val compileTasksNames =
            listOf("Jvm6", "NodeJs", "Metadata", "Wasm32", hostTargetName.capitalize()).map { ":compileKotlin$it" }

        with(libProject) {
            build("publish") {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray(), ":jvm6Jar", ":nodeJsJar", ":metadataJar")

                val groupDir = projectDir.resolve("repo/com/example")
                val jvmJarName = "sample-lib-jvm6/1.0/sample-lib-jvm6-1.0.jar"
                val jsJarName = "sample-lib-nodejs/1.0/sample-lib-nodejs-1.0.jar"
                val metadataJarName = "sample-lib-metadata/1.0/sample-lib-metadata-1.0.jar"
                val wasmKlibName = "sample-lib-wasm32/1.0/sample-lib-wasm32-1.0.klib"
                val nativeKlibName = "sample-lib-$hostTargetName/1.0/sample-lib-$hostTargetName-1.0.klib"

                listOf(jvmJarName, jsJarName, metadataJarName, "sample-lib/1.0/sample-lib-1.0.module").forEach {
                    Assert.assertTrue("$it should exist", groupDir.resolve(it).exists())
                }

                val jvmJarEntries = ZipFile(groupDir.resolve(jvmJarName)).entries().asSequence().map { it.name }.toSet()
                Assert.assertTrue("com/example/lib/CommonKt.class" in jvmJarEntries)
                Assert.assertTrue("com/example/lib/MainKt.class" in jvmJarEntries)

                val jsJar = ZipFile(groupDir.resolve(jsJarName))
                val compiledJs = jsJar.getInputStream(jsJar.getEntry("sample-lib.js")).reader().readText()
                Assert.assertTrue("function id(" in compiledJs)
                Assert.assertTrue("function idUsage(" in compiledJs)
                Assert.assertTrue("function expectedFun(" in compiledJs)
                Assert.assertTrue("function main(" in compiledJs)

                val metadataJarEntries = ZipFile(groupDir.resolve(metadataJarName)).entries().asSequence().map { it.name }.toSet()
                Assert.assertTrue("com/example/lib/CommonKt.kotlin_metadata" in metadataJarEntries)

                Assert.assertTrue(groupDir.resolve(wasmKlibName).exists())
                Assert.assertTrue(groupDir.resolve(nativeKlibName).exists())
            }
        }

        val libLocalRepoUri = libProject.projectDir.resolve("repo").toURI()

        with(appProject) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nrepositories { maven { url '$libLocalRepoUri' } }")

            fun CompiledProject.checkAppBuild() {
                assertSuccessful()
                assertTasksExecuted(*compileTasksNames.toTypedArray())

                projectDir.resolve(targetClassesDir("jvm6")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/UseBothIdsKt.class").exists())
                }

                projectDir.resolve(targetClassesDir("jvm8")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/UseBothIdsKt.class").exists())
                    Assert.assertTrue(resolve("com/example/app/Jdk8ApiUsageKt.class").exists())
                }

                projectDir.resolve(targetClassesDir("metadata")).run {
                    Assert.assertTrue(resolve("com/example/app/AKt.kotlin_metadata").exists())
                }

                projectDir.resolve(targetClassesDir("nodeJs")).resolve("sample-app.js").readText().run {
                    Assert.assertTrue(contains("console.info"))
                    Assert.assertTrue(contains("function nodeJsMain("))
                }

                projectDir.resolve(targetClassesDir("wasm32")).run {
                    Assert.assertTrue(resolve("sample-app.klib").exists())
                }

                assertFileExists("build/bin/wasm32/main/debug/executable/sample_app.wasm.js")
                assertFileExists("build/bin/wasm32/main/debug/executable/sample_app.wasm")
                assertFileExists("build/bin/wasm32/main/release/executable/sample_app.wasm.js")
                assertFileExists("build/bin/wasm32/main/release/executable/sample_app.wasm")

                val nativeExeName = if (isWindows) "sample-app.exe" else "sample-app.kexe"
                assertFileExists("build/bin/$hostTargetName/main/release/executable/$nativeExeName")
                assertFileExists("build/bin/$hostTargetName/main/debug/executable/$nativeExeName")
            }

            build("assemble", "resolveRuntimeDependencies") {
                checkAppBuild()
                assertTasksExecuted(":resolveRuntimeDependencies") // KT-26301
            }

            // Now run again with a project dependency instead of a module one:
            libProject.projectDir.copyRecursively(projectDir.resolve(libProject.projectDir.name))
            projectDir.resolve("settings.gradle").appendText("\ninclude '${libProject.projectDir.name}'")
            gradleBuildScript().modify { it.replace("'com.example:sample-lib:1.0'", "project(':${libProject.projectDir.name}')") }

            build("clean", "assemble", "--rerun-tasks") {
                checkAppBuild()
            }
        }

        with(oldStyleAppProject) {
            setupWorkingDir()
            gradleBuildScript().appendText("\nallprojects { repositories { maven { url '$libLocalRepoUri' } } }")

            build("assemble") {
                assertSuccessful()
                assertTasksExecuted(":app-js:compileKotlin2Js", ":app-jvm:compileKotlin", ":app-common:compileKotlinCommon")

                assertFileExists(kotlinClassesDir("app-common") + "com/example/app/CommonAppKt.kotlin_metadata")

                val jvmClassFile = projectDir.resolve(kotlinClassesDir("app-jvm") + "com/example/app/JvmAppKt.class")
                checkBytecodeContains(jvmClassFile, "CommonKt.id", "MainKt.expectedFun")

                val jsCompiledFilePath = kotlinClassesDir("app-js") + "app-js.js"
                assertFileContains(jsCompiledFilePath, "lib.expectedFun", "lib.id")
            }
        }
    }

    @Test
    fun testSourceSetCyclicDependencyDetection() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()
        gradleBuildScript().appendText("\n" + """
            kotlin.sourceSets {
                a
                b { dependsOn a }
                c { dependsOn b }
                a.dependsOn(c)
            }
        """.trimIndent())

        build("assemble") {
            assertFailed()
            assertContains("a -> c -> b -> a")
        }
    }

    @Test
    fun testJvmWithJavaEquivalence() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        lateinit var classesWithoutJava: Set<String>

        fun getFilePathsSet(inDirectory: String): Set<String> {
            val dir = projectDir.resolve(inDirectory)
            return dir.walk().filter { it.isFile }.map { it.relativeTo(dir).path.replace('\\', '/') }.toSet()
        }

        build("assemble") {
            assertSuccessful()
            classesWithoutJava = getFilePathsSet("build/classes")
        }

        gradleBuildScript().modify { it.replace("presets.jvm", "presets.jvmWithJava") }

        projectDir.resolve("src/main/java").apply {
            mkdirs()
            mkdir()
            // Check that Java can access the dependencies (kotlin-stdlib):
            resolve("JavaClassInJava.java").writeText("""
                package com.example.lib;
                import kotlin.sequences.Sequence;
                class JavaClassInJava {
                    Sequence<String> makeSequence() { throw new UnsupportedOperationException(); }
                }
            """.trimIndent())

            // Add a Kotlin source file in the Java source root and check that it is compiled:
            resolve("KotlinClassInJava.kt").writeText("""
                package com.example.lib
                class KotlinClassInJava
            """.trimIndent())
        }

        build("clean", "assemble") {
            assertSuccessful()
            val expectedClasses =
                classesWithoutJava +
                        "kotlin/jvm6/main/com/example/lib/KotlinClassInJava.class" +
                        "java/main/com/example/lib/JavaClassInJava.class"
            val actualClasses = getFilePathsSet("build/classes")
            Assert.assertEquals(expectedClasses, actualClasses)
        }
    }

    @Test
    fun testLibWithTests() = with(Project("new-mpp-lib-with-tests", gradleVersion)) {
        build("check") {
            assertSuccessful()
            assertTasksExecuted(
                // compilation tasks:
                ":compileKotlinJs",
                ":compileTestKotlinJs",
                ":compileKotlinJvmWithoutJava",
                ":compileTestKotlinJvmWithoutJava",
                ":compileKotlinJvmWithJava",
                ":compileJava",
                ":compileTestKotlinJvmWithJava",
                ":compileTestJava",
                // test tasks:
                ":jsTest", // does not run any actual tests for now
                ":jvmWithoutJavaTest",
                ":test"
            )

            val expectedKotlinOutputFiles = listOf(
                kotlinClassesDir(sourceSet = "js/main") + "new-mpp-lib-with-tests.js",
                kotlinClassesDir(sourceSet = "js/test") + "new-mpp-lib-with-tests_test.js",
                *kotlinClassesDir(sourceSet = "jvmWithJava/main").let {
                    arrayOf(
                        it + "com/example/lib/JavaClassUsageKt.class",
                        it + "com/example/lib/CommonKt.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module"
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithJava/test").let {
                    arrayOf(
                        it + "com/example/lib/TestCommonCode.class",
                        it + "com/example/lib/TestWithJava.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module" // Note: same name as in main
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithoutJava/main").let {
                    arrayOf(
                        it + "com/example/lib/CommonKt.class",
                        it + "com/example/lib/MainKt.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module"
                    )
                },
                *kotlinClassesDir(sourceSet = "jvmWithoutJava/test").let {
                    arrayOf(
                        it + "com/example/lib/TestCommonCode.class",
                        it + "com/example/lib/TestWithoutJava.class",
                        it + "META-INF/new-mpp-lib-with-tests.kotlin_module" // Note: same name as in main
                    )
                }
            )

            expectedKotlinOutputFiles.forEach { assertFileExists(it) }
        }
    }

    @Test
    fun testLanguageSettingsApplied() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets.jvm6Main.languageSettings {
                    languageVersion = '1.3'
                    apiVersion = '1.3'
                    enableLanguageFeature('InlineClasses')
                    progressiveMode = true
                }
            """.trimIndent()
        )

        build("compileKotlinJvm6") {
            assertSuccessful()
            assertContains("-language-version 1.3", "-api-version 1.3", "-XXLanguage:+InlineClasses", " -Xprogressive")
        }
    }

    @Test
    fun testLanguageSettingsConsistency() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        setupWorkingDir()

        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets {
                    foo { }
                    bar { dependsOn foo }
                }
            """.trimIndent()
        )

        fun testMonotonousCheck(sourceSetConfigurationChange: String, expectedErrorHint: String) {
            gradleBuildScript().appendText("\nkotlin.sourceSets.foo.${sourceSetConfigurationChange}")
            build("tasks") {
                assertFailed()
                assertContains(expectedErrorHint)
            }
            gradleBuildScript().appendText("\nkotlin.sourceSets.bar.${sourceSetConfigurationChange}")
            build("tasks") {
                assertSuccessful()
            }
        }

        testMonotonousCheck("languageSettings.languageVersion = '1.4'", SourceSetConsistencyChecks.languageVersionCheckHint)
        testMonotonousCheck("languageSettings.enableLanguageFeature('InlineClasses')", SourceSetConsistencyChecks.unstableFeaturesHint)

        // check that enabling a bugfix feature and progressive mode or advancing API level
        // don't require doing the same for dependent source sets:
        gradleBuildScript().appendText(
            "\n" + """
                kotlin.sourceSets.foo.languageSettings {
                    apiVersion = '1.3'
                    enableLanguageFeature('SoundSmartcastForEnumEntries')
                    progressiveMode = true
                }
            """.trimIndent()
        )
        build("tasks") {
            assertSuccessful()
        }
    }

    @Test
    fun testResolveMppLibDependencyToMetadata() {
        val libProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val appProject = Project("sample-app", gradleVersion, "new-mpp-lib-and-app")

        libProject.build("publish") { assertSuccessful() }
        val localRepo = libProject.projectDir.resolve("repo")
        val localRepoUri = localRepo.toURI()

        with(appProject) {
            setupWorkingDir()

            val pathPrefix = "metadataDependency: "

            gradleBuildScript().appendText(
                "\n" + """
                    repositories { maven { url '$localRepoUri' } }

                    kotlin.sourceSets {
                        commonMain {
                            dependencies {
                                // add these dependencies to check that they are resolved to metadata
                                api 'com.example:sample-lib:1.0'
                                compileOnly 'com.example:sample-lib:1.0'
                                runtimeOnly 'com.example:sample-lib:1.0'
                            }
                        }
                    }

                    task('printMetadataFiles') {
                        doFirst {
                            ['Api', 'Implementation', 'CompileOnly', 'RuntimeOnly'].each { kind ->
                                def configuration = configurations.getByName("commonMain${'$'}kind" + '$METADATA_CONFIGURATION_NAME_SUFFIX')
                                configuration.files.each { println '$pathPrefix' + configuration.name + '->' + it.name }
                            }
                        }
                    }
                """.trimIndent()
            )
            val metadataDependencyRegex = "$pathPrefix(.*?)->(.*)".toRegex()

            build("printMetadataFiles") {
                assertSuccessful()

                val expectedFileName = "sample-lib-${KotlinMultiplatformPlugin.METADATA_TARGET_NAME}-1.0.jar"

                val paths = metadataDependencyRegex.findAll(output).map { it.groupValues[1] to it.groupValues[2] }.toSet()

                Assert.assertEquals(
                    listOf("Api", "Implementation", "CompileOnly", "RuntimeOnly").map {
                        "commonMain$it$METADATA_CONFIGURATION_NAME_SUFFIX" to expectedFileName
                    }.toSet(),
                    paths
                )
            }
        }
    }

    @Test
    fun testPublishingOnlySupportedNativeTargets() = with(Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")) {
        val (publishedVariant, nonPublishedVariant) = when {
            HostManager.hostIsMac -> "macos64" to "linux64"
            HostManager.hostIsLinux -> "linux64" to "macos64"
            HostManager.hostIsMingw -> "mingw64" to "linux64"
            else -> error("Unknown host")
        }

        build("publish") {
            assertSuccessful()

            assertFileExists("repo/com/example/sample-lib-$publishedVariant/1.0/sample-lib-$publishedVariant-1.0.klib")
            assertNoSuchFile("repo/com/example/sample-lib-$nonPublishedVariant") // check that no artifacts are published for that variant

            // but check that the module metadata contains both variants:
            val gradleModuleMetadata = projectDir.resolve("repo/com/example/sample-lib/1.0/sample-lib-1.0.module").readText()
            assertTrue(""""name": "linux64-api"""" in gradleModuleMetadata)
            assertTrue(""""name": "mingw64-api"""" in gradleModuleMetadata)
        }
    }

    @Test
    fun testCanProduceNativeLibraries() = with(Project("new-mpp-native-libraries", gradleVersion)) {
        val hostTargetName = when {
            HostManager.hostIsMingw -> "mingw64"
            HostManager.hostIsLinux -> "linux64"
            HostManager.hostIsMac -> "macos64"
            else -> error("Unknown host")
        }
        val baseName = "native_lib"

        val sharedPrefix = CompilerOutputKind.DYNAMIC.prefix(HostManager.host)
        val sharedSuffix = CompilerOutputKind.DYNAMIC.suffix(HostManager.host)
        val sharedPaths = listOf(
            "build/bin/$hostTargetName/main/debug/shared/$sharedPrefix$baseName$sharedSuffix",
            "build/bin/$hostTargetName/main/release/shared/$sharedPrefix$baseName$sharedSuffix"
        )

        val staticPrefix = CompilerOutputKind.STATIC.prefix(HostManager.host)
        val staticSuffix = CompilerOutputKind.STATIC.suffix(HostManager.host)
        val staticPaths = listOf(
            "build/bin/$hostTargetName/main/debug/static/$staticPrefix$baseName$staticSuffix",
            "build/bin/$hostTargetName/main/release/static/$staticPrefix$baseName$staticSuffix"
        )

        val headerPaths = listOf(
            "build/bin/$hostTargetName/main/debug/shared/$sharedPrefix${baseName}_api.h",
            "build/bin/$hostTargetName/main/release/shared/$sharedPrefix${baseName}_api.h",
            "build/bin/$hostTargetName/main/debug/static/$staticPrefix${baseName}_api.h",
            "build/bin/$hostTargetName/main/release/static/$staticPrefix${baseName}_api.h"
        )

        val taskSuffix = hostTargetName.capitalize()
        val linkTasks = listOf(
            ":linkMainDebugShared$taskSuffix",
            ":linkMainReleaseShared$taskSuffix",
            ":linkMainDebugStatic$taskSuffix",
            ":linkMainReleaseStatic$taskSuffix"
        )

        build("assemble") {
            assertSuccessful()

            sharedPaths.forEach { assertFileExists(it) }
            staticPaths.forEach { assertFileExists(it) }
            headerPaths.forEach { assertFileExists(it) }
        }

        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks)
        }

        assertTrue(projectDir.resolve(headerPaths[0]).delete())

        build("assemble") {
            assertSuccessful()
            assertTasksUpToDate(linkTasks.drop(1))
            assertTasksExecuted(linkTasks[0])
        }

    }
}