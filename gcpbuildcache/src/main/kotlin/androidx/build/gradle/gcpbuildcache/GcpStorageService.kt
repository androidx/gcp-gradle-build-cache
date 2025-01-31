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

import androidx.build.gradle.core.FileHandleInputStream
import androidx.build.gradle.core.FileHandleInputStream.Companion.handleInputStream
import androidx.build.gradle.core.StorageService
import androidx.build.gradle.core.TokenInfoService
import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.http.HttpTransportOptions
import com.google.cloud.storage.*
import org.gradle.api.GradleException
import org.gradle.api.logging.Logging
import java.io.InputStream
import java.time.Clock
import java.time.OffsetDateTime


/**
 * An implementation of the [StorageService] that is backed by Google Cloud Storage.
 */
internal class GcpStorageService(
    private val projectId: String,
    override val bucketName: String,
    gcpCredentials: GcpCredentials,
    messageOnAuthenticationFailure: String,
    override val isPush: Boolean,
    override val isEnabled: Boolean,
    private val sizeThreshold: Long = BLOB_SIZE_THRESHOLD,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StorageService {

    private val storageOptions: StorageOptions? by lazy {
        storageOptions(projectId, gcpCredentials, messageOnAuthenticationFailure, isPush)
    }

    override fun load(cacheKey: String): InputStream? {
        if (!isEnabled) {
            logger.info("Not Enabled")
            return null
        }
        val storageOptions = storageOptions ?: return null
        val blobId = BlobId.of(bucketName, cacheKey)
        logger.info("Loading $cacheKey from ${blobId.name}")
        val content = load(storageOptions, blobId, sizeThreshold)
        update(storageOptions, blobId, OffsetDateTime.now(clock))
        return content
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
        return store(storageOptions, blobId, contents, OffsetDateTime.now(clock))
    }

    override fun delete(cacheKey: String): Boolean {
        if (!isEnabled) {
            return false
        }

        if (!isPush) {
            return false
        }
        val blobId = BlobId.of(bucketName, cacheKey)
        return delete(storageOptions, blobId)
    }

    override fun validateConfiguration() {
        if (storageOptions?.service?.get(bucketName, Storage.BucketGetOption.fields()) == null) {
            error("Bucket $bucketName under project $projectId cannot be found or it is not accessible using the provided credentials.")
        }
    }

    override fun close() {
        // Does nothing
    }

    companion object {
        private val logger = Logging.getLogger("GcpStorageService")

        private val transportOptions by lazy {
            GcpTransportOptions(HttpTransportOptions.newBuilder())
        }

        // The OAuth scopes for reading and writing to buckets.
        // https://cloud.google.com/storage/docs/authentication
        private const val STORAGE_READ_ONLY = "https://www.googleapis.com/auth/devstorage.read_only"
        private const val STORAGE_READ_WRITE = "https://www.googleapis.com/auth/devstorage.read_write"

        // Need full control for updating metadata
        private const val STORAGE_FULL_CONTROL = "https://www.googleapis.com/auth/devstorage.full_control"

        private const val BLOB_SIZE_THRESHOLD = 50 * 1024 * 1024L
        private const val BUFFER_SIZE = 32 * 1024 * 1024

        private fun load(storage: StorageOptions?, blobId: BlobId, sizeThreshold: Long): InputStream? {
            if (storage == null) return null
            return try {
                val blob = storage.service.get(blobId) ?: return null
                return if (blob.size == 0L) {
                    // return empty entries as a cache miss
                    return null
                } else if (blob.size > sizeThreshold) {
                    val path = FileHandleInputStream.create()
                    blob.downloadTo(path)
                    // Always return a buffered stream
                    path.handleInputStream()
                        .buffered(bufferSize = BUFFER_SIZE)
                } else {
                    blob.getContent().inputStream()
                        .buffered(bufferSize = BUFFER_SIZE)
                }
            } catch (storageException: StorageException) {
                logger.debug("Unable to load Blob ($blobId)", storageException)
                null
            }
        }

        private fun store(
            storage: StorageOptions?,
            blobId: BlobId,
            contents: ByteArray,
            customTime: OffsetDateTime,
        ): Boolean {
            if (storage == null) return false
            val blobInfo = BlobInfo.newBuilder(blobId)
                .setCustomTimeOffsetDateTime(customTime)
                .build()
            return try {
                storage.service.createFrom(blobInfo, contents.inputStream())
                true
            } catch (storageException: StorageException) {
                logger.debug("Unable to store Blob ($blobId)", storageException)
                false
            }
        }

        private fun update(
            storage: StorageOptions,
            blobId: BlobId,
            customTime: OffsetDateTime,
        ) {
            val blob = storage.service.get(blobId) ?: return
            if (blob.customTimeOffsetDateTime < customTime) {
                logger.info("Updating Custom-Time for ${blobId.name} to $customTime")
                blob.toBuilder()
                    .setCustomTimeOffsetDateTime(customTime)
                    .build()
                storage.service.update(blob)
            }
        }

        private fun delete(storage: StorageOptions?, blobId: BlobId): Boolean {
            if (storage == null) return false
            return storage.service.delete(blobId)
        }

        private fun storageOptions(
            projectId: String,
            gcpCredentials: GcpCredentials,
            messageOnAuthenticationFailure: String,
            isPushSupported: Boolean
        ): StorageOptions? {
            val credentials = credentials(
                gcpCredentials,
                messageOnAuthenticationFailure,
                isPushSupported
            ) ?: return null
            val retrySettings = RetrySettings.newBuilder()
            retrySettings.maxAttempts = 3
            return StorageOptions.newBuilder().setCredentials(credentials)
                .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy()).setProjectId(projectId)
                .setRetrySettings(retrySettings.build())
                .setTransportOptions(transportOptions)
                .build()
        }

        /**
         * Attempts to use reflection to clear the cached credentials inside the Google authentication library.
         */
        private fun clearCachedDefaultCredentials() {
            try {
                val field = GoogleCredentials::class.java.getDeclaredField("defaultCredentialsProvider")
                field.isAccessible = true
                val defaultCredentialsProvider = field.get(null)
                val cachedCredentials = field.type.getDeclaredField("cachedCredentials")
                cachedCredentials.isAccessible = true
                cachedCredentials.set(defaultCredentialsProvider, null)
            } catch (exception: Exception) {
                // unable to clear the credentials, oh well.
            }
        }

        private fun defaultApplicationGcpCredentials(
            scopes: List<String>,
            messageOnAuthenticationFailure: String,
            forceClearCache: Boolean
        ): GoogleCredentials {
            if (forceClearCache) clearCachedDefaultCredentials()
            val credentials = GoogleCredentials.getApplicationDefault().createScoped(scopes)

            try {
                // If the credentials have expired,
                // reauth is required by the user to be able to generate or refresh access token;
                // Refreshing the access token here helps us to provide a useful error message to the user
                // in case the credentials have expired
                credentials.refreshIfExpired()
            } catch (e: Exception) {
                if (forceClearCache) {
                    throw Exception(messageOnAuthenticationFailure)
                } else {
                    return defaultApplicationGcpCredentials(
                        scopes,
                        messageOnAuthenticationFailure,
                        forceClearCache = true
                    )
                }
            }
            val tokenService = TokenInfoService.tokenService()
            val tokenInfoResponse = tokenService.tokenInfo(credentials.accessToken.tokenValue).execute()
            if (!tokenInfoResponse.isSuccessful) {
                if (forceClearCache) {
                    throw Exception(messageOnAuthenticationFailure)
                } else {
                    return defaultApplicationGcpCredentials(
                        scopes,
                        messageOnAuthenticationFailure,
                        forceClearCache = true
                    )
                }
            }
            return credentials
        }

        private fun credentials(
            gcpCredentials: GcpCredentials,
            messageOnAuthenticationFailure: String,
            isPushSupported: Boolean
        ): GoogleCredentials? {
            val scopes = buildList {
                add(STORAGE_READ_ONLY)
                if (isPushSupported) {
                    add(STORAGE_READ_WRITE)
                    add(STORAGE_FULL_CONTROL)
                }
            }
            return when (gcpCredentials) {
                is ApplicationDefaultGcpCredentials -> {
                    defaultApplicationGcpCredentials(
                        scopes,
                        messageOnAuthenticationFailure,
                        forceClearCache = false
                    )
                }

                is ExportedKeyGcpCredentials -> {
                    val contents = gcpCredentials.credentials.invoke()
                    if (contents.isBlank()) throw GradleException("Credentials are empty.")
                    // Use the underlying transport factory to ensure logging is disabled.
                    val credentials = GoogleCredentials.fromStream(
                        contents.byteInputStream(charset = Charsets.UTF_8),
                        transportOptions.httpTransportFactory
                    ).createScoped(scopes)
                    try {
                        // If the credentials have expired,
                        // reauth is required by the user to be able to generate or refresh access token;
                        // Refreshing the access token here helps us to provide a useful error message to the user
                        // in case the credentials have expired
                        credentials.refreshIfExpired()
                    } catch (e: Exception) {
                        error("Your GCP Credentials have expired. Please regenerate credentials and try again: gcloud auth application-default login")
                    }
                    val tokenService = TokenInfoService.tokenService()
                    val tokenInfoResponse = tokenService.tokenInfo(credentials.accessToken.tokenValue).execute()
                    if (!tokenInfoResponse.isSuccessful) {
                        throw GradleException(tokenInfoResponse.errorBody().toString())
                    }
                    credentials
                }
            }
        }
    }
}
