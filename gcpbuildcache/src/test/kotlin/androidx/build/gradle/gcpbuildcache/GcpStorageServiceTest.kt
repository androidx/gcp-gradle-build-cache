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

    @Test
    fun testStoreBlob() {
        Assume.assumeNotNull(serviceAccountPath)
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = File(serviceAccountPath!!),
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
            serviceAccountPath = File(serviceAccountPath!!),
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
        Assume.assumeNotNull(serviceAccountPath)
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = File(serviceAccountPath!!),
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
            serviceAccountPath = File(serviceAccountPath!!),
            isPush = true,
            isEnabled = true
        )
        val readOnlyStorageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            serviceAccountPath = File(serviceAccountPath),
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
            serviceAccountPath = File(serviceAccountPath!!),
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
