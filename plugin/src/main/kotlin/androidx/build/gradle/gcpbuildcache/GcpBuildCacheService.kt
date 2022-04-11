package androidx.build.gradle.gcpbuildcache

import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ReadChannel
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.StorageRetryStrategy
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.channels.Channels

/**
 * The service that responds to Gradle's request to load and store results for a given
 * [BuildCacheKey].
 *
 * @param projectId The Google Cloud Platform project id, that can be used for billing.
 * @param bucketName The name of the bucket that is used to store all the gradle cache entries.
 * This essentially becomes the root of all cache entries.
 */
class GcpBuildCacheService(private val projectId: String, private val bucketName: String) : BuildCacheService {
    override fun close() {
        // Does nothing
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        val blobId = BlobId.of(bucketName, key.blobKey())
        val readChannel = load(projectId, blobId) ?: return false
        // Stream the contents of the blob
        val input = Channels.newInputStream(readChannel)
        reader.readFrom(input)
        return true
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        val blobId = BlobId.of(bucketName, key.blobKey())
        val output = ByteArrayOutputStream()
        output.use {
            writer.writeTo(output)
        }
        store(projectId, blobId, output.toByteArray())
    }

    companion object {
        // The path to the service account credentials
        private const val ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH = "ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH"

        // The OAuth scopes for reading and writing to buckets.
        // https://cloud.google.com/storage/docs/authentication
        private const val STORAGE_READ_ONLY = "https://www.googleapis.com/auth/devstorage.read_only"
        private const val STORAGE_READ_WRITE = "https://www.googleapis.com/auth/devstorage.read_write"
        // Need full control for updating metadata
        private const val STORAGE_FULL_CONTROL = "https://www.googleapis.com/auth/devstorage.full_control"

        internal fun load(projectId: String, blobId: BlobId): ReadChannel? {
            val storage = storageOptions(projectId) ?: return null
            var blob = storage.service.get(blobId) ?: return null
            val blobInfo = blob.toBuilder().setCustomTime(System.currentTimeMillis()).build()
            blob = blobInfo.update()
            return blob.reader()
        }

        internal fun store(projectId: String, blobId: BlobId, contents: ByteArray): Boolean {
            val storage = storageOptions(projectId) ?: return false
            val blobInfo = BlobInfo.newBuilder(blobId)
                .setCustomTime(System.currentTimeMillis())
                .build()
            storage.service.createFrom(blobInfo, contents.inputStream())
            return true
        }

        internal fun deleteForTest(projectId: String, blobId: BlobId): Boolean {
            val storage = storageOptions(projectId) ?: return false
            return storage.service.delete(blobId)
        }

        private fun storageOptions(
            projectId: String
        ): StorageOptions? {
            val credentials = credentials() ?: return null
            val retrySettings = RetrySettings.newBuilder()
            retrySettings.maxAttempts = 3
            retrySettings.retryDelayMultiplier = 2.0
            return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy())
                .setProjectId(projectId)
                .setRetrySettings(retrySettings.build())
                .build()
        }

        private fun credentials(writer: Boolean = false): GoogleCredentials? {
            val path = serviceAccountPath() ?: return null
            val scopes = listOf(STORAGE_READ_ONLY, STORAGE_READ_WRITE, STORAGE_FULL_CONTROL)
            val credentials = GoogleCredentials.fromStream(path.inputStream())
                .createScoped(scopes)
            credentials.refreshIfExpired()
            return credentials
        }

        /**
         * @return The [File] path to the service account keys.
         */
        private fun serviceAccountPath(): File? {
            val path = System.getenv()[ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH]
            if (path != null) {
                val file = File(path)
                if (file.isFile && file.exists()) {
                    return file
                }
            }
            return null
        }

        // Build Cache Key Helpers

        private val SLASHES = """"[/]+""".toRegex()

        internal fun BuildCacheKey.blobKey(): String {
            return hashCode.replace(SLASHES, "/")
        }
    }
}
