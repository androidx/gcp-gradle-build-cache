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

import com.adobe.testing.s3mock.S3MockApplication
import com.adobe.testing.s3mock.S3MockApplication.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

/**
 * This test relies on a mock implementation of an AWS S3 server to run
 * */
class S3StorageServiceTest {

    private lateinit var s3MockApplication: S3MockApplication
    private lateinit var client: S3Client

    @Before
    fun setUp() {
        val serverConfig = mapOf(
            PROP_INITIAL_BUCKETS to BUCKET_NAME,
            PROP_SILENT to true
        )
        s3MockApplication = start(serverConfig)
        val serviceEndpoint = "http://localhost:$DEFAULT_HTTP_PORT"
        client = S3Client.builder()
            .region(Region.of(REGION))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .endpointOverride(URI.create(serviceEndpoint))
            .forcePathStyle(true) // https://github.com/adobe/S3Mock/issues/880
            .build()
    }

    @After
    fun tearDown() {
        s3MockApplication.stop()
        client.close()
    }

    @Test
    fun testStoreBlob() {
        val storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            client = client,
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
            client = client,
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
            client = client,
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
            client = client,
            isPush = true,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD
        )
        val readOnlyStorageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            client = client,
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
            client = client,
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

        private const val REGION = "us-east-1"
        private const val BUCKET_NAME = "bucket-name"
        private const val SIZE_THRESHOLD = 50 * 1024 * 1024L
    }
}
