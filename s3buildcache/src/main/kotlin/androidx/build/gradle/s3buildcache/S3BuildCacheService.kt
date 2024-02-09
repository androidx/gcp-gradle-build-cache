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

import androidx.build.gradle.core.FileSystemStorageService
import androidx.build.gradle.core.blobKey
import org.gradle.api.logging.Logging
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.ByteArrayOutputStream

/**
 * The service that responds to Gradle's request to load and store results for a given
 * [BuildCacheKey].
 *
 * @param region The AWS region the S3 bucket is located in.
 * @param bucketName The name of the bucket that is used to store all the gradle cache entries.
 * @param reducedRedundancy Whether to use reduced redundancy.
 * This essentially becomes the root of all cache entries.
 */
class S3BuildCacheService(
    credentials: S3Credentials,
    region: String,
    bucketName: String,
    isPush: Boolean,
    isEnabled: Boolean,
    reducedRedundancy: Boolean,
    inTestMode: Boolean = false
) : BuildCacheService {

    private val client by lazy {
        clientOptions(credentials(credentials), region)
    }
    private val storageService = if (inTestMode) {
        FileSystemStorageService(bucketName, isPush, isEnabled)
    } else {
        S3StorageService(bucketName, isPush, isEnabled, client, region, reducedRedundancy)
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        logger.info("Loading ${key.blobKey()}")
        val cacheKey = key.blobKey()
        val input = storageService.load(cacheKey) ?: return false
        reader.readFrom(input)
        return true
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        if (writer.size == 0L) return // do not store empty entries into the cache
        logger.info("Storing ${key.blobKey()}")
        val cacheKey = key.blobKey()
        val output = ByteArrayOutputStream()
        output.use {
            writer.writeTo(output)
        }
        storageService.store(cacheKey, output.toByteArray())
    }

    override fun close() {
        storageService.close()
    }

    fun validateConfiguration() {
        storageService.validateConfiguration()
    }

    companion object {

        private val logger by lazy {
            Logging.getLogger("AwsS3BuildCacheService")
        }

        private fun clientOptions(credentials: AwsCredentialsProvider, region: String): S3Client {
            return S3Client.builder()
                .credentialsProvider(credentials)
                .region(Region.of(region))
                .build()
        }

        private fun credentials(s3Credentials: S3Credentials): AwsCredentialsProvider {
            return when (s3Credentials) {
                DefaultS3Credentials -> DefaultCredentialsProvider.create()
                is SpecificCredentialsProvider -> s3Credentials.provider
                is ProfileS3Credentials -> ProfileCredentialsProvider.create(s3Credentials.profile)
                is ExportedS3Credentials -> StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Credentials.awsAccessKeyId, s3Credentials.awsSecretKey)
                )
            }
        }
    }
}
