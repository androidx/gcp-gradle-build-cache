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
    private val sizeThreshold: Long = BLOB_SIZE_THRESHOLD
) : StorageService {

    private val storageOptions by lazy {
        storageOptions(projectId, gcpCredentials, messageOnAuthenticationFailure, isPush)
    }

    override fun load(cacheKey: String): InputStream? {
        if (!isEnabled) {
            logger.info("Not Enabled")
            return null
        }
        val blobId = BlobId.of(bucketName, cacheKey)
        logger.info("Loading $cacheKey from ${blobId.name}")
        return load(storageOptions, blobId, sizeThreshold)
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
        return delete(storageOptions, blobId)
    }

    override fun validateConfiguration() {
        if (storageOptions?.service?.get(bucketName, Storage.BucketGetOption.fields()) == null) {
            throw Exception("""
                Bucket $bucketName under project $projectId cannot be found or it is not accessible using the provided
                credentials.
                """.trimIndent()
            )
        }
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

        private const val BLOB_SIZE_THRESHOLD = 50 * 1024 * 1024L

        private fun load(storage: StorageOptions?, blobId: BlobId, sizeThreshold: Long): InputStream? {
            if (storage == null) return null
            return try {
                val blob = storage.service.get(blobId) ?: return null
                return if (blob.size > sizeThreshold) {
                    val path = FileHandleInputStream.create()
                    blob.downloadTo(path)
                    path.handleInputStream()
                } else {
                    blob.getContent().inputStream()
                }
            } catch (storageException: StorageException) {
                logger.debug("Unable to load Blob ($blobId)", storageException)
                null
            }
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
            val scopes = mutableListOf(
                STORAGE_READ_ONLY,
            )
            if (isPushSupported) {
                scopes += listOf(STORAGE_READ_WRITE, STORAGE_FULL_CONTROL)
            }
            return when (gcpCredentials) {
                is ApplicationDefaultGcpCredentials -> {
                    defaultApplicationGcpCredentials(scopes, messageOnAuthenticationFailure, forceClearCache = false)
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
                        throw GradleException("""
                            "Your GCP Credentials have expired.
                            Please regenerate credentials and try again.
                            """.trimIndent()
                        )
                    }
                    val tokenService = TokenInfoService.tokenService()
                    val tokenInfoResponse = tokenService.tokenInfo(credentials.accessToken.tokenValue).execute()
                    if(!tokenInfoResponse.isSuccessful) {
                        throw GradleException(tokenInfoResponse.errorBody().toString())
                    }
                    credentials
                }
            }
        }
    }
}
