package androidx.build.gradle.gcpbuildcache

import com.google.cloud.storage.BlobId
import org.junit.Test
import java.nio.channels.Channels

class GcpBuildCacheServiceTest {
    @Test
    fun testStoreBlob() {
        val blobId = BlobId.of(BUCKET_NAME, "test-store.txt")
        val contents = "The quick brown fox jumped over the lazy dog"
        val result = GcpBuildCacheService.store(PROJECT_ID, blobId, contents.toByteArray(Charsets.UTF_8))
        assert(result)
        GcpBuildCacheService.deleteForTest(PROJECT_ID, blobId)
    }

    @Test
    fun testLoadBlob() {
        val blobId = BlobId.of(BUCKET_NAME, "test-load.txt")
        val contents = "The quick brown fox jumped over the lazy dog"
        val bytes = contents.toByteArray(Charsets.UTF_8)
        GcpBuildCacheService.store(PROJECT_ID, blobId, bytes)
        val readChannel = GcpBuildCacheService.load(PROJECT_ID, blobId)!!
        val input = Channels.newInputStream(readChannel)
        val result = String(input.readAllBytes(), Charsets.UTF_8)
        assert(result == contents)
        GcpBuildCacheService.deleteForTest(PROJECT_ID, blobId)
    }

    companion object {
        // The bucket name where all the artifacts are stored
        private const val BUCKET_NAME = "androidx-gradle-build-cache-test"

        // The GCP project
        private const val PROJECT_ID = "androidx-dev-prod"
    }
}
