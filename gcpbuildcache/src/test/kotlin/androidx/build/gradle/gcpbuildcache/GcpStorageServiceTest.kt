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

package androidx.build.gradle.gcpbuildcache

import org.gradle.api.Transformer
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.util.function.BiFunction

/**
 * These tests only run with GRADLE_CACHE_SERVICE_ACCOUNT_PATH set
 */
class GcpStorageServiceTest {
    private val serviceAccountPath = System.getenv()["GRADLE_CACHE_SERVICE_ACCOUNT_PATH"]
    private val serviceAccountPathProperty: RegularFileProperty = FakeRegularFileProperty(serviceAccountPath)

    @Test
    fun testStoreBlob() {
        Assume.assumeNotNull(serviceAccountPath)
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = serviceAccountPathProperty,
            isPush = true,
            isEnabled = true
        )
        storageService.use {
            val cacheKey = "test-store.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(result)
            storageService.delete(cacheKey)
        }
    }

    @Test
    fun testLoadBlob() {
        Assume.assumeNotNull(serviceAccountPath)
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = serviceAccountPathProperty,
            isPush = true,
            isEnabled = true
        )
        storageService.use {
            val cacheKey = "test-load.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val bytes = contents.toByteArray(Charsets.UTF_8)
            assert(storageService.store(cacheKey, bytes))
            val input = storageService.load(cacheKey)!!
            val result = String(input.readAllBytes(), Charsets.UTF_8)
            assert(result == contents)
            storageService.delete(cacheKey)
        }
    }

    @Test
    fun testStoreBlob_noPushSupport() {
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = serviceAccountPathProperty,
            isPush = false,
            isEnabled = true
        )
        storageService.use {
            val cacheKey = "test-store-no-push.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(!result)
        }
    }

    @Test
    fun testLoadBlob_noPushSupport() {
        Assume.assumeNotNull(serviceAccountPath)
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = serviceAccountPathProperty,
            isPush = true,
            isEnabled = true
        )
        val readOnlyStorageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = serviceAccountPathProperty,
            isPush = false,
            isEnabled = true
        )
        storageService.use {
            readOnlyStorageService.use {
                val cacheKey = "test-load-no-push.txt"
                val contents = "The quick brown fox jumped over the lazy dog"
                val bytes = contents.toByteArray(Charsets.UTF_8)
                assert(storageService.store(cacheKey, bytes))
                val input = readOnlyStorageService.load(cacheKey)!!
                val result = String(input.readAllBytes(), Charsets.UTF_8)
                assert(result == contents)
                storageService.delete(cacheKey)
            }
        }
    }

    @Test
    fun testLoadBlob_disabled() {
        Assume.assumeNotNull(serviceAccountPath)
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = serviceAccountPathProperty,
            isPush = true,
            isEnabled = false
        )
        storageService.use {
            val cacheKey = "test-store-disabled.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(!result)
        }
    }

    companion object {
        // Project ID
        private const val PROJECT_ID = "androidx-dev-prod"

        // The Bucket Name
        private const val BUCKET_NAME = "androidx-gradle-build-cache-test"
    }
}

private class FakeRegularFileProperty(private val path: String?): RegularFileProperty {
    override fun isPresent(): Boolean = path != null
    override fun getAsFile(): Provider<File> = object : Provider<File> {
        override fun get(): File = File(path!!)

        // The rest is not implemented as it is not used
        override fun getOrNull(): File? = TODO("Not yet implemented")
        override fun getOrElse(defaultValue: File): File = TODO("Not yet implemented")
        override fun <S : Any?> map(transformer: Transformer<out S, in File>): Provider<S> {
            TODO("Not yet implemented")
        }
        override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S>, in File>): Provider<S> {
            TODO("Not yet implemented")
        }
        override fun isPresent(): Boolean = TODO("Not yet implemented")
        override fun orElse(value: File): Provider<File> = TODO("Not yet implemented")
        override fun orElse(p0: Provider<out File>): Provider<File> = TODO("Not yet implemented")
        @Suppress("OVERRIDE_DEPRECATION")
        override fun forUseAtConfigurationTime(): Provider<File> = TODO("Not yet implemented")
        override fun <B : Any?, R : Any?> zip(p0: Provider<B>, p1: BiFunction<File, B, R>): Provider<R> {
            TODO("Not yet implemented")
        }
    }

    // The rest is not implemented as it is not used
    override fun get(): RegularFile = TODO("Not yet implemented")
    override fun getOrNull(): RegularFile? = TODO("Not yet implemented")
    override fun getOrElse(defaultValue: RegularFile): RegularFile = TODO("Not yet implemented")
    override fun <S : Any?> map(transformer: Transformer<out S, in RegularFile>): Provider<S> {
        TODO("Not yet implemented")
    }
    override fun <S : Any?> flatMap(transformer: Transformer<out Provider<out S>, in RegularFile>): Provider<S> {
        TODO("Not yet implemented")
    }
    override fun orElse(value: RegularFile): Provider<RegularFile> = TODO("Not yet implemented")
    override fun orElse(p0: Provider<out RegularFile>): Provider<RegularFile> = TODO("Not yet implemented")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun forUseAtConfigurationTime(): Provider<RegularFile> = TODO("Not yet implemented")
    override fun <B : Any?, R : Any?> zip(p0: Provider<B>, p1: BiFunction<RegularFile, B, R>): Provider<R> {
        TODO("Not yet implemented")
    }
    override fun finalizeValue() = TODO("Not yet implemented")
    override fun finalizeValueOnRead() = TODO("Not yet implemented")
    override fun disallowChanges() = TODO("Not yet implemented")
    override fun disallowUnsafeRead() = TODO("Not yet implemented")
    override fun set(file: File?) = TODO("Not yet implemented")
    override fun set(value: RegularFile?) = TODO("Not yet implemented")
    override fun set(provider: Provider<out RegularFile>) = TODO("Not yet implemented")
    override fun value(value: RegularFile?): RegularFileProperty = TODO("Not yet implemented")
    override fun value(provider: Provider<out RegularFile>): RegularFileProperty = TODO("Not yet implemented")
    override fun convention(value: RegularFile?): RegularFileProperty = TODO("Not yet implemented")
    override fun convention(provider: Provider<out RegularFile>): RegularFileProperty {
        TODO("Not yet implemented")
    }
    override fun fileValue(file: File?): RegularFileProperty = TODO("Not yet implemented")
    override fun fileProvider(provider: Provider<File>): RegularFileProperty = TODO("Not yet implemented")
    override fun getLocationOnly(): Provider<RegularFile> = TODO("Not yet implemented")
}