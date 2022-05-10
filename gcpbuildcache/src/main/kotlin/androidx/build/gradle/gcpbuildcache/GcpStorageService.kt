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

import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ReadChannel
import com.google.cloud.http.HttpTransportOptions
import com.google.cloud.storage.*
import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import java.io.InputStream
import java.nio.channels.Channels

/**
 * An implementation of the [StorageService] that is backed by Google Cloud Storage.
 */
internal class GcpStorageService(
    override val projectId: String,
    override val bucketName: String,
    gcpCredentials: GcpCredentials,
    override val isPush: Boolean,
    override val isEnabled: Boolean,
) : StorageService {

    private val storageOptions by lazy { storageOptions(projectId, gcpCredentials, isPush) }

    override fun load(cacheKey: String): InputStream? {
        if (!isEnabled) {
            logger.info("Not Enabled")
            return null
        }

        val blobId = BlobId.of(bucketName, cacheKey)
        logger.info("Loading $cacheKey from ${blobId.name}")
        val readChannel = load(storageOptions, blobId) ?: return null
        return Channels.newInputStream(readChannel)
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
        val blobId = BlobId.of(bucketName, cacheKey)

        logger.info("Storing $cacheKey into ${blobId.name}")
        return store(storageOptions, blobId, contents)
    }

    override fun delete(cacheKey: String): Boolean {
        if (!isEnabled) {
            return false
        }

        if (!isPush) {
            return false
        }
        val blobId = BlobId.of(bucketName, cacheKey)
        return Companion.delete(storageOptions, blobId)
    }

    override fun close() {
        // Does nothing
    }

    companion object {

        private val logger by lazy {
            Logging.getLogger("GcpStorageService")
        }

        private val transportOptions by lazy {
            GcpTransportOptions(HttpTransportOptions.newBuilder())
        }

        // The OAuth scopes for reading and writing to buckets.
        // https://cloud.google.com/storage/docs/authentication
        private const val STORAGE_READ_ONLY = "https://www.googleapis.com/auth/devstorage.read_only"
        private const val STORAGE_READ_WRITE = "https://www.googleapis.com/auth/devstorage.read_write"

        // Need full control for updating metadata
        private const val STORAGE_FULL_CONTROL = "https://www.googleapis.com/auth/devstorage.full_control"

        private fun load(storage: StorageOptions?, blobId: BlobId): ReadChannel? {
            if (storage == null) return null
            val blob = storage.service.get(blobId) ?: return null
            val reader = blob.reader()
            // We don't expect to store objects larger than Int.MAX_VALUE
            reader.setChunkSize(blob.size.toInt())
            return reader
        }

        private fun store(storage: StorageOptions?, blobId: BlobId, contents: ByteArray): Boolean {
            if (storage == null) return false
            val blobInfo = BlobInfo.newBuilder(blobId).build()
            return try {
                storage.service.createFrom(blobInfo, contents.inputStream())
                true
            } catch (storageException: StorageException) {
                logger.debug("Unable to store Blob ($blobId)", storageException)
                false
            }
        }

        private fun delete(storage: StorageOptions?, blobId: BlobId): Boolean {
            if (storage == null) return false
            return storage.service.delete(blobId)
        }

        private fun storageOptions(
            projectId: String,
            gcpCredentials: GcpCredentials,
            isPushSupported: Boolean
        ): StorageOptions? {
            val credentials = credentials(gcpCredentials, isPushSupported) ?: return null
            val retrySettings = RetrySettings.newBuilder()
            // We don't want retries.
            retrySettings.maxAttempts = 0
            return StorageOptions.newBuilder().setCredentials(credentials)
                .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy()).setProjectId(projectId)
                .setRetrySettings(retrySettings.build())
                .setTransportOptions(transportOptions)
                .build()
        }

        private fun credentials(
            gcpCredentials: GcpCredentials,
            isPushSupported: Boolean
        ): GoogleCredentials? {
            val scopes = mutableListOf(
                STORAGE_READ_ONLY,
            )
            if (isPushSupported) {
                scopes += listOf(STORAGE_READ_WRITE, STORAGE_FULL_CONTROL)
            }
            return when (gcpCredentials) {
                is ApplicationDefaultGcpCredentials -> {
                    GoogleCredentials.getApplicationDefault().createScoped(scopes)
                }
                is ExportedKeyGcpCredentials -> {
                    val contents = gcpCredentials.credentials.invoke()
                    if (contents.isBlank()) throw GradleException("Credentials are empty.")
                    // Use the underlying transport factory to ensure logging is disabled.
                    GoogleCredentials.fromStream(
                        contents.byteInputStream(charset = Charsets.UTF_8),
                        transportOptions.httpTransportFactory
                    ).createScoped(scopes)
                }
            }
        }
    }
}
