plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.cloud:google-cloud-storage:2.6.0")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest()
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            // Use Kotlin Test test framework
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

gradlePlugin {
    plugins {
        create("gcpbuildcache") {
            id = "androidx.build.gradle.gcpbuildcache"
            displayName = "Gradle GCP Build Cache Plugin"
            description = """
                Implementation of Gradle Build Cache that allows to use Google Cloud Platform
                storage buckets as a back end.
            """.trimIndent()
            implementationClass = "androidx.build.gradle.gcpbuildcache.GcpGradleBuildCachePlugin"
        }
    }
}

group = "androidx.build.gradle.gcpbuildcache"
version = "1.0.0-alpha01"

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
