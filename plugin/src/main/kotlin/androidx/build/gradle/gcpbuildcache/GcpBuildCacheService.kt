package androidx.build.gradle.gcpbuildcache

import com.google.cloud.storage.Bucket
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService

/**
 * The service that responds to Gradle's request to load and store results for a given
 * [BuildCacheKey].
 */
class GcpBuildCacheService(private val bucket: Bucket) : BuildCacheService {
  override fun close() {
    // Does nothing
  }

  override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
    val storage = GcpStorageService()
    return storage.load(key, reader)
  }

  override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
    val storage = GcpStorageService()
    storage.store(key, writer)
  }
}