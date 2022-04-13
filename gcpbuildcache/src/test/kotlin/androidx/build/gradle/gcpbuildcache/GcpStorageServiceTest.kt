package androidx.build.gradle.gcpbuildcache

import org.junit.Test

class GcpStorageServiceTest {
    @Test
    fun testStoreBlob() {
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            isPush = true
        )
        storageService.use {
            val cacheKey = "test-store.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(result)
            storageService.delete(cacheKey)
        }
    }

    @Test
    fun testLoadBlob() {
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            isPush = true
        )
        storageService.use {
            val cacheKey = "test-load.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val bytes = contents.toByteArray(Charsets.UTF_8)
            assert(storageService.store(cacheKey, bytes))
            val input = storageService.load(cacheKey)!!
            val result = String(input.readAllBytes(), Charsets.UTF_8)
            assert(result == contents)
            storageService.delete(cacheKey)
        }
    }

    @Test
    fun testStoreBlob_noPushSupport() {
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            isPush = false
        )
        storageService.use {
            val cacheKey = "test-store.txt"
            val contents = "The quick brown fox jumped over the lazy dog"
            val result = storageService.store(cacheKey, contents.toByteArray(Charsets.UTF_8))
            assert(!result)
        }
    }

    @Test
    fun testLoadBlob_noPushSupport() {
        val storageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            isPush = true
        )
        val readOnlyStorageService = GcpStorageService(
            projectId = PROJECT_ID,
            bucketName = BUCKET_NAME,
            isPush = false
        )
        storageService.use {
            readOnlyStorageService.use {
                val cacheKey = "test-load.txt"
                val contents = "The quick brown fox jumped over the lazy dog"
                val bytes = contents.toByteArray(Charsets.UTF_8)
                assert(storageService.store(cacheKey, bytes))
                val input = readOnlyStorageService.load(cacheKey)!!
                val result = String(input.readAllBytes(), Charsets.UTF_8)
                assert(result == contents)
                storageService.delete(cacheKey)
            }
        }
    }

    companion object {
        // Project ID
        private const val PROJECT_ID = "androidx-dev-prod"

        // The Bucket Name
        private const val BUCKET_NAME = "androidx-gradle-build-cache-test"
    }
}
