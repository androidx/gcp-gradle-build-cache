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

package androidx.build.gradle.core

import org.junit.Test

class FileStorageServiceTest {
    @Test
    fun testStoreBlob() {
        val storageService = FileSystemStorageService(
            bucketName = BUCKET_NAME,
            prefix = null,
            isPush = true,
            isEnabled = true
        )
        storageService.use {
            val cacheKey = "test-store.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(result)
        }
    }

    @Test
    fun testStoreBlob_withPrefix() {
        val storageService = FileSystemStorageService(
            bucketName = BUCKET_NAME,
            prefix = "test-prefix",
            isPush = true,
            isEnabled = true
        )
        storageService.use {
            val cacheKey = "test-store.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(result)
        }
    }

    @Test
    fun testLoadBlob() {
        val storageService = FileSystemStorageService(
            bucketName = BUCKET_NAME,
            prefix = null,
            isPush = true,
            isEnabled = true
        )
        storageService.use {
            val cacheKey = "test-load.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val bytes = contents.toByteArray(Charsets.UTF_8)
            storageService.store(cacheKey, bytes)
            val input = storageService.load(cacheKey)!!
            val result = String(input.readAllBytes(), Charsets.UTF_8)
            assert(result == contents)
        }
    }

    @Test
    fun testStoreBlob_noPushSupport() {
        val storageService = FileSystemStorageService(
            bucketName = BUCKET_NAME,
            prefix = null,
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
    fun testStoreBlob_disabled() {
        val storageService = FileSystemStorageService(
            bucketName = BUCKET_NAME,
            prefix = null,
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
        private const val BUCKET_NAME = "cache"
    }
}
