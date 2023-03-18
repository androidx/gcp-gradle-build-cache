/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

plugins {
    id("maven-publish")
    id("signing")
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.amazon.bom))
    implementation(libs.amazon.s3)
    implementation(libs.amazon.sso)
    // This has to be on the classpath to be able to read credentials. See: https://github.com/aws/aws-sdk-java/issues/1324
    runtimeOnly(libs.amazon.sts)
}

pluginBundle {
    website = "https://github.com/androidx/gcp-gradle-build-cache"
    vcsUrl = "https://github.com/androidx/gcp-gradle-build-cache"
    tags = listOf("buildcache", "s3", "caching")
}

gradlePlugin {
    plugins {
        create("s3buildcache") {
            id = "androidx.build.gradle.s3buildcache"
            displayName = "Gradle AWS S3 Build Cache Plugin"
            description = "Gradle remote build cache backed by AWS S3"
            implementationClass = "androidx.build.gradle.s3buildcache.S3GradleBuildCachePlugin"
        }
    }
}

group = "androidx.build.gradle.s3buildcache"
version = "1.0.0-beta01"

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test Framework
            useKotlinTest()
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            // Use Kotlin Test Framework
            useKotlinTest()

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project)
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.hasProperty("signing.keyId") }
}