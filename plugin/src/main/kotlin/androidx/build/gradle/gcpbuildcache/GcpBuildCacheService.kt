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
class GcpBuildCacheService(bucket: Bucket) : BuildCacheService {
  override fun close() {
    TODO("Not yet implemented")
  }

  override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
    TODO("Not yet implemented")
  }

  override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
    TODO("Not yet implemented")
  }
}