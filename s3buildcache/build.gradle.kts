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

// Bundle core library directly as we only get to publish one jar per plugin in Gradle Plugin Portal
sourceSets.main {
    java.srcDir("../core/src/main/kotlin")
}

dependencies {
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(platform(libs.amazon.bom))
    implementation(libs.amazon.s3)
    implementation(libs.amazon.sso)
    runtimeOnly(libs.amazon.sts) {
        because("Has to be on the classpath to be able to read credentials. See: https://github.com/aws/aws-sdk-java/issues/1324")
    }
    testImplementation(libs.adobe.s3.mock) {
        // Classpath collisions
        exclude("ch.qos.logback", "logback-classic")
    }
}

gradlePlugin {
    website.set("https://github.com/androidx/gcp-gradle-build-cache")
    vcsUrl.set("https://github.com/androidx/gcp-gradle-build-cache")
    plugins {
        create("s3buildcache") {
            id = "androidx.build.gradle.s3buildcache"
            displayName = "Gradle AWS S3 Build Cache Plugin"
            description = "Gradle remote build cache backed by AWS S3"
            implementationClass = "androidx.build.gradle.s3buildcache.S3GradleBuildCachePlugin"
            tags.set(listOf("buildcache", "s3", "caching"))
        }
    }
}

group = "androidx.build.gradle.s3buildcache"
version = "1.0.0-alpha02"

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            useJUnit()
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            useJUnit()

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project())
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