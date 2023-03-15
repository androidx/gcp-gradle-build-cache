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

import androidx.build.gradle.core.StorageService
import org.gradle.api.logging.Logging
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.StorageClass.REDUCED_REDUNDANCY
import software.amazon.awssdk.services.s3.model.StorageClass.STANDARD
import java.io.InputStream

class S3StorageService(
    credentials: S3Credentials,
    override val bucketName: String,
    override val isPush: Boolean,
    override val isEnabled: Boolean,
    private val region: String,
    private val reducedRedundancy: Boolean,
    private val sizeThreshold: Long = BLOB_SIZE_THRESHOLD
) : StorageService {

    private val client by lazy { clientOptions(credentials(credentials), region) }

    override fun load(cacheKey: String): InputStream? {
        if (!isEnabled) {
            logger.info("Not Enabled")
            return null
        }

        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(cacheKey)
            .build()
        logger.info("Loading $cacheKey via $request")
        return load(client, request, sizeThreshold)
    }

    override fun store(cacheKey: String, contents: ByteArray): Boolean {
        if (!isEnabled) {
            logger.info("Not Enabled")
            return false
        }

        if (!isPush) {
            logger.info("No push support")
            return false
        }

        if (contents.size > sizeThreshold) {
            logger.info("Cache item $cacheKey size is ${contents.size} and it exceeds $sizeThreshold. Will skip storing")
            return false
        }

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(cacheKey)
            .storageClass(if (reducedRedundancy) REDUCED_REDUNDANCY else STANDARD)
            .build()
        logger.info("Storing $cacheKey via $request")
        return store(client, request, contents)
    }

    override fun delete(cacheKey: String): Boolean {
        if (!isEnabled) {
            logger.info("Not Enabled")
            return false
        }

        if (!isPush) {
            logger.info("No push support")
            return false
        }

        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(cacheKey)
            .build()
        logger.info("Deleting $cacheKey via $request")
        return delete(client, request)
    }

    override fun validateConfiguration() {
        val buckets = client.listBuckets().buckets()
        if (buckets.none { bucket -> bucket.name() == bucketName }) {
            throw Exception("Bucket $bucketName under project $region cannot be found or it is not accessible using the provided credentials")
        }
    }

    override fun close() {
        client.close()
    }

    companion object {

        private const val BLOB_SIZE_THRESHOLD = 50 * 1024 * 1024L

        private val logger by lazy {
            Logging.getLogger("AwsS3StorageService")
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
                is ExportedS3Credentials -> StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3Credentials.awsAccessKeyId, s3Credentials.awsSecretKey)
                )
            }
        }

        private fun load(
            client: S3Client,
            request: GetObjectRequest,
            sizeThreshold: Long,
        ): InputStream? {
            return try {
                val inputStream = client.getObject(request)
                val blob = inputStream.response() ?: return null
                if (blob.contentLength() > sizeThreshold) {
                    logger.info("Cache item ${request.key()} size is ${blob.contentLength()} and it exceeds $sizeThreshold. Will skip loading")
                    inputStream.abort()
                    null
                } else {
                    inputStream
                }
            } catch (e: Exception) {
                logger.debug("Unable to load $request", e)
                null
            }
        }

        private fun store(
            client: S3Client,
            request: PutObjectRequest,
            contents: ByteArray
        ): Boolean {
            return try {
                client.putObject(request, RequestBody.fromBytes(contents))
                true
            } catch (e: Exception) {
                logger.debug("Unable to store $request", e)
                false
            }
        }

        private fun delete(client: S3Client, request: DeleteObjectRequest): Boolean {
            return client.deleteObject(request).deleteMarker()
        }
    }
}
