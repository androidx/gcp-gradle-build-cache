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
import java.io.File
import java.io.InputStream
import java.nio.channels.Channels

/**
 * An implementation of the [StorageService] that is backed by Google Cloud Storage.
 */
class GcpStorageService(
    override val projectId: String,
    override val bucketName: String,
    override val isPush: Boolean
) : StorageService {

    private val storageOptions by lazy { storageOptions(projectId, isPush) }

    override fun load(cacheKey: String): InputStream? {
        val blobId = BlobId.of(bucketName, cacheKey)
        val readChannel = load(storageOptions, blobId) ?: return null
        return Channels.newInputStream(readChannel)
    }

    override fun store(cacheKey: String, contents: ByteArray): Boolean {
        if (!isPush) {
            return false
        }
        val blobId = BlobId.of(bucketName, cacheKey)
        return store(storageOptions, blobId, contents)
    }

    override fun delete(cacheKey: String): Boolean {
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
        // The path to the service account credentials
        private const val GRADLE_CACHE_SERVICE_ACCOUNT_PATH = "GRADLE_CACHE_SERVICE_ACCOUNT_PATH"

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
            isPushSupported: Boolean
        ): StorageOptions? {
            val credentials = credentials(isPushSupported) ?: return null
            val retrySettings = RetrySettings.newBuilder()
            retrySettings.maxAttempts = 3
            retrySettings.retryDelayMultiplier = 2.0
            return StorageOptions.newBuilder().setCredentials(credentials)
                .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy()).setProjectId(projectId)
                .setRetrySettings(retrySettings.build()).build()
        }

        private fun credentials(isPushSupported: Boolean): GoogleCredentials? {
            val path = serviceAccountPath() ?: return null
            val scopes = mutableListOf(
                STORAGE_READ_ONLY,
            )
            if (isPushSupported) {
                scopes += listOf(STORAGE_READ_WRITE, STORAGE_FULL_CONTROL)
            }
            return GoogleCredentials.fromStream(path.inputStream()).createScoped(scopes)
        }

        /**
         * @return The [File] path to the service account keys.
         */
        private fun serviceAccountPath(): File? {
            val path = System.getenv()[GRADLE_CACHE_SERVICE_ACCOUNT_PATH]
            if (path != null) {
                val file = File(path)
                if (file.isFile && file.exists()) {
                    return file
                }
            }
            return null
        }
    }
}
