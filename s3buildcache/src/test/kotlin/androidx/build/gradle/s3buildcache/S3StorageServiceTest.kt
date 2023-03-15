/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.gradle.s3buildcache

import org.junit.Test

/**
 * This test needs to be configured with the correct values for REGION, BUCKET_NAME,
 * and AWS credentials, therefore, it won't pass on CI as is.
 * */
class S3StorageServiceTest {

    @Test
    fun testStoreBlob() {
        val storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            credentials = DefaultS3Credentials,
            isPush = true,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        storageService.use {
            val cacheKey = "test-store.txt"
            val contents = "The quick brown fox jumps over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(result)
            storageService.delete(cacheKey)
        }
    }

    @Test
    fun testLoadBlob() {
        val storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            credentials = DefaultS3Credentials,
            isPush = true,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        storageService.use {
            val cacheKey = "test-load.txt"
            val contents = "The quick brown fox jumps over the lazy dog"
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
        val storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            credentials = DefaultS3Credentials,
            isPush = false,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        storageService.use {
            val cacheKey = "test-store-no-push.txt"
            val contents = "The quick brown fox jumps over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(!result)
        }
    }

    @Test
    fun testLoadBlob_noPushSupport() {
        val storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            credentials = DefaultS3Credentials,
            isPush = true,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        val readOnlyStorageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            credentials = DefaultS3Credentials,
            isPush = false,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        storageService.use {
            readOnlyStorageService.use {
                val cacheKey = "test-load-no-push.txt"
                val contents = "The quick brown fox jumps over the lazy dog"
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
        val storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            credentials = DefaultS3Credentials,
            isPush = true,
            isEnabled = false,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        storageService.use {
            val cacheKey = "test-store-disabled.txt"
            val contents = "The quick brown fox jumps over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(!result)
        }
    }

    companion object {

        private const val REGION = "region"
        private const val BUCKET_NAME = "bucket-name"
        private const val SIZE_THRESHOLD = 50 * 1024 * 1024L
    }
}
