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
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.StorageRetryStrategy
import org.gradle.api.GradleException
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
            return null
        }

        val blobId = BlobId.of(bucketName, cacheKey)
        val readChannel = load(storageOptions, blobId) ?: return null
        return Channels.newInputStream(readChannel)
    }

    override fun store(cacheKey: String, contents: ByteArray): Boolean {
        if (!isEnabled) {
            return false
        }

        if (!isPush) {
            return false
        }
        val blobId = BlobId.of(bucketName, cacheKey)
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
        // The OAuth scopes for reading and writing to buckets.
        // https://cloud.google.com/storage/docs/authentication
        private const val STORAGE_READ_ONLY = "https://www.googleapis.com/auth/devstorage.read_only"
        private const val STORAGE_READ_WRITE = "https://www.googleapis.com/auth/devstorage.read_write"

        // Need full control for updating metadata
        private const val STORAGE_FULL_CONTROL = "https://www.googleapis.com/auth/devstorage.full_control"

        private fun load(storage: StorageOptions?, blobId: BlobId): ReadChannel? {
            if (storage == null) return null
            val blob = storage.service.get(blobId) ?: return null
            return blob.reader()
        }

        private fun store(storage: StorageOptions?, blobId: BlobId, contents: ByteArray): Boolean {
            if (storage == null) return false
            val blobInfo = BlobInfo.newBuilder(blobId).build()
            storage.service.createFrom(blobInfo, contents.inputStream())
            return true
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
            retrySettings.maxAttempts = 3
            retrySettings.retryDelayMultiplier = 2.0
            return StorageOptions.newBuilder().setCredentials(credentials)
                .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy()).setProjectId(projectId)
                .setRetrySettings(retrySettings.build()).build()
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
                    val path = gcpCredentials.pathToCredentials
                    if (!path.exists()) throw GradleException("Your specified $path does not exist")
                    if (!path.isFile) throw GradleException("Your specified $path is not a file")

                    GoogleCredentials.fromStream(path.inputStream()).createScoped(scopes)
                }
            }
        }
    }
}
