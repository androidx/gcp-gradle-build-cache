package androidx.build.gradle.gcpbuildcache

import com.google.api.gax.retrying.RetrySettings
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import com.google.cloud.storage.StorageRetryStrategy
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.channels.Channels
import java.text.SimpleDateFormat
import java.util.*

/**
 * The storage service that can authenticate with cloud storage and read and write to the build cache.
 */
class GcpStorageService {
    fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        val storage = storageOptions(writer = true) ?: return false
        val blobId = BlobId.of(BUCKET_NAME, key.blobKey())
        val metadata = mutableMapOf(
            CONTENT_TYPE_KEY to CONTENT_TYPE,
            LAST_ACCESSED_AT to now()
        )
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setMetadata(metadata)
            .build()

        val blob = storage.service.get(blobId) ?: return false
        // Update Metadata
        storage.service.update(blobInfo)
        // Stream the contents of the blob
        val readChannel = blob.reader()
        val input = Channels.newInputStream(readChannel)
        reader.readFrom(input)
        return true
    }

    fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter): Boolean {
        val storage = storageOptions(writer = true) ?: return false
        val blobId = BlobId.of(BUCKET_NAME, key.blobKey())
        val metadata = mutableMapOf(
            CONTENT_TYPE_KEY to CONTENT_TYPE,
            LAST_ACCESSED_AT to now()
        )
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setMetadata(metadata)
            .build()

        // Investigate the use of a PipedOutputStream
        // To avoid having to copy things into an intermediate byte array
        val output = ByteArrayOutputStream()
        output.use {
            writer.writeTo(output)
        }
        storage.service.createFrom(blobInfo, output.toByteArray().inputStream())
        return true
    }

    companion object {
        // The GCP project
        private const val PROJECT_ID = "androidx-dev-prod"

        // The bucket name where all the artifacts are stored
        private const val BUCKET_NAME = "androidx-gradle-build-cache"

        // The path to the service account credentials
        private const val ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH = "ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH"

        // The custom metadata updated everytime we read an entity.
        // This helps with defining object lifecycles.
        private const val LAST_ACCESSED_AT = "LAST_ACCESSED_AT"
        private const val CONTENT_TYPE_KEY = "ContentType"
        private const val CONTENT_TYPE = "GRADLE_BUILD_CACHE_ENTRY"

        // The OAuth scopes for reading and writing to buckets.
        private const val STORAGE_READ_ONLY = "https://www.googleapis.com/auth/devstorage.read_only"
        private const val STORAGE_READ_WRITE = "https://www.googleapis.com/auth/devstorage.read_write"

        private val DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z")

        fun storageOptions(writer: Boolean = false): StorageOptions? {
            val credentials = credentials(writer = writer) ?: return null
            val retrySettings = RetrySettings.newBuilder()
            retrySettings.maxAttempts = 3
            retrySettings.retryDelayMultiplier = 2.0
            return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setStorageRetryStrategy(StorageRetryStrategy.getUniformStorageRetryStrategy())
                .setProjectId(PROJECT_ID)
                .setRetrySettings(retrySettings.build())
                .build()
        }

        private fun credentials(writer: Boolean = false): GoogleCredentials? {
            val path = serviceAccountPath() ?: return null
            val scopes = mutableListOf(STORAGE_READ_ONLY)
            if (writer) {
                scopes += STORAGE_READ_WRITE
            }
            val credentials = GoogleCredentials.fromStream(path.inputStream())
                .createScoped(scopes)
            credentials.refreshIfExpired()
            return credentials
        }

        /**
         * @return The [File] path to the service account keys.
         */
        private fun serviceAccountPath(): File? {
            val environment = System.getenv()
            val path = environment[ANDROIDX_GRADLE_SERVICE_ACCOUNT_PATH]
            if (path != null) {
                val file = File(path)
                if (file.isFile && file.exists()) {
                    return file
                }
            }
            return null
        }

        private fun now(): String {
            return DATE_FORMAT.format(Date())
        }

        // Build Cache Key Helpers

        private val SLASHES = """"[/]+""".toRegex()

        fun BuildCacheKey.blobKey(): String {
            return hashCode.replace(SLASHES, "/")
        }

    }
}
